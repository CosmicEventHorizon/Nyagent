package com.pirouette.nyagent.presentation.activity

import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pirouette.nyagent.NyagentApplication
import com.pirouette.nyagent.R
import com.pirouette.nyagent.application.model.ChatMessageModel
import com.pirouette.nyagent.application.model.MessageAuthorModel
import com.pirouette.nyagent.application.model.OllamaMessageModel
import com.pirouette.nyagent.application.model.SavedPromptModel
import com.pirouette.nyagent.application.model.SavedStoryModel
import com.pirouette.nyagent.application.service.ChatService
import com.pirouette.nyagent.application.service.StoryService
import com.pirouette.nyagent.infrastructure.linux.LinuxEnvironmentService
import com.pirouette.nyagent.presentation.adapter.ChatAdapter
import com.pirouette.nyagent.presentation.adapter.StoryAdapter

class MainActivity : AppCompatActivity() {

    private companion object {
        const val STATE_MESSAGES = "state_messages"
        const val STATE_CONVERSATION_LOG = "state_conversation_log"
    }

    private lateinit var chatService: ChatService
    private lateinit var storyService: StoryService
    private lateinit var environmentService: LinuxEnvironmentService

    private lateinit var btnSend: ImageButton
    private lateinit var btnEditClose: ImageButton
    private lateinit var editBar: View
    private lateinit var etPrompt: EditText
    private lateinit var contextBar: View
    private lateinit var lblContext: TextView
    private var editMessageIndex: Int = -1
    private lateinit var recyclerview: RecyclerView
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val locator = (application as NyagentApplication).serviceLocator
        chatService = locator.chatService
        storyService = locator.storyService
        environmentService = locator.environmentService

        createExternalFolder()
        ensureEnvironmentInstalled()

        btnSend = findViewById(R.id.btnSendText)
        btnEditClose = findViewById(R.id.btnEditClose)
        editBar = findViewById(R.id.editBar)
        contextBar = findViewById(R.id.contextBar)
        lblContext = findViewById(R.id.lblContext)
        etPrompt = findViewById(R.id.etPrompt)
        recyclerview = findViewById(R.id.rvMessages)
        recyclerview.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        adapter = ChatAdapter(chatService.messages) { index ->
            enterEditMode(index)
        }
        recyclerview.adapter = adapter

        chatService.listener = serviceListener
        restoreConversationState(savedInstanceState)
        updateContextBar()
        updateSendButton()

        btnSend.setOnClickListener { onSendClicked() }
        btnEditClose.setOnClickListener { exitEditMode() }
    }

    /** Installs the Linux environment on first boot, without blocking the UI. */
    private fun ensureEnvironmentInstalled() {
        if (environmentService.isInstalled) {
            return
        }
        val installer = Thread {
            environmentService.install()
        }
        installer.isDaemon = true
        installer.start()
    }

    private val serviceListener = object : ChatService.Listener {
        override fun onMessagesChanged() {
            updateSendButton()
            updateContextBar()
            adapter.notifyDataSetChanged()
            scrollToBottom()
        }

        override fun onAssistantDelta(content: String) {
            updateContextBar()
            adapter.notifyDataSetChanged()
            scrollToBottom()
        }

        override fun onError(message: String) {
            updateContextBar()
            adapter.notifyDataSetChanged()
            scrollToBottom()
        }

        override fun onContextChanged(percentUsed: Int) {
            updateContextBar()
            updateSendButton()
        }
    }

    private fun onSendClicked() {
        if (chatService.isRunning) {
            chatService.stop()
            updateSendButton()
            return
        }
        val content = etPrompt.text.toString().trim()
        if (content.isEmpty()) return
        if (editMessageIndex >= 0) {
            chatService.editAndRestart(editMessageIndex, content)
            editMessageIndex = -1
            editBar.visibility = View.GONE
        } else {
            chatService.sendUserMessage(content)
        }
        etPrompt.setText("")
        updateSendButton()
        adapter.notifyDataSetChanged()
        scrollToBottom()
    }

    /** Switch the send button to Stop while a harness loop is running. */
    private fun updateSendButton() {
        if (chatService.isCompacting) {
            btnSend.setImageResource(R.drawable.icon_send)
            btnSend.setBackgroundResource(R.drawable.send_button_background)
            btnSend.isEnabled = false
        } else if (chatService.isRunning) {
            btnSend.setImageResource(R.drawable.icon_stop)
            btnSend.setBackgroundResource(R.drawable.stop_button_background)
            btnSend.isEnabled = true
        } else {
            btnSend.setImageResource(R.drawable.icon_send)
            btnSend.setBackgroundResource(R.drawable.send_button_background)
            btnSend.isEnabled = true
        }
    }

    /** Shows how much of the configured context window is left, in white on black. */
    private fun updateContextBar() {
        val percentLeft = 100 - chatService.contextPercentUsed
        lblContext.setText("Context left: $percentLeft%")
        contextBar.visibility = View.VISIBLE
        etPrompt.isEnabled = !chatService.isCompacting && !chatService.isRunning
    }

    /** Enters edit mode for a previous user message: pre-fills the prompt and shows the close button. */
    private fun enterEditMode(index: Int) {
        if (index < 0 || index >= chatService.messages.size) return
        val message = chatService.messages[index]
        if (message.author != MessageAuthorModel.USER) return
        editMessageIndex = index
        etPrompt.setText(message.content)
        editBar.visibility = View.VISIBLE
        etPrompt.requestFocus()
    }

    /** Leaves edit mode, returning to normal mode with an empty prompt. */
    private fun exitEditMode() {
        editMessageIndex = -1
        etPrompt.setText("")
        editBar.visibility = View.GONE
        etPrompt.clearFocus()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(STATE_MESSAGES, ArrayList(chatService.messages))
        outState.putSerializable(STATE_CONVERSATION_LOG, ArrayList(chatService.conversationLog))
    }

    private fun restoreConversationState(state: Bundle?) {
        @Suppress("unchecked_cast")
        val messages = state?.getSerializable(STATE_MESSAGES) as ArrayList<ChatMessageModel>?
        @Suppress("unchecked_cast")
        val conversationLog = state?.getSerializable(STATE_CONVERSATION_LOG) as ArrayList<OllamaMessageModel>?

        var savedMessages: ArrayList<ChatMessageModel>? = null
        var savedConversationLog: ArrayList<OllamaMessageModel>? = null

        if (messages != null && conversationLog != null) {
            if (messages.isNotEmpty() && messages.first().author == MessageAuthorModel.SYSTEM) {
                val removeCount = if (messages.size > 1 && messages[1].author == MessageAuthorModel.USER) 2 else 1
                savedMessages = ArrayList(messages.subList(removeCount, messages.size))
            } else {
                savedMessages = messages
            }
            savedConversationLog = conversationLog
        }

        if (savedMessages != null && savedConversationLog != null) {
            chatService.restoreState(savedMessages, savedConversationLog)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.opSettings -> {
                startActivity(android.content.Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.opSave -> {
                showSavePopup()
                true
            }
            R.id.opClear -> {
                chatService.clearConversation()
                true
            }
            R.id.opLoad -> {
                showLoadPopup()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLoadPopup() {
        val inflater = LayoutInflater.from(this)
        val popupView: View = inflater.inflate(R.layout.activity_load, null)
        val container: RecyclerView = popupView.findViewById(R.id.rvLoads)
        val btnDeleteStory: Button = popupView.findViewById(R.id.btnDeleteStory)
        val btnLoadStory: Button = popupView.findViewById(R.id.btnLoadStory)

        val stories = storyService.loadAll().toMutableList()
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

        container.layoutManager = LinearLayoutManager(this)
        val popupAdapter = StoryAdapter(stories)
        container.adapter = popupAdapter

        btnLoadStory.setOnClickListener {
            val position = popupAdapter.selectedPosition
            if (position in stories.indices) {
                chatService.loadStory(stories[position])
                toast("${stories[position].name} Loaded!")
            }
        }

        btnDeleteStory.setOnClickListener {
            val position = popupAdapter.selectedPosition
            if (position in stories.indices) {
                stories.removeAt(position)
                storyService.delete(position)
                popupAdapter.selectedPosition = RecyclerView.NO_POSITION
                popupAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun showSavePopup() {
        val inflater = LayoutInflater.from(this)
        val popupSaveView: View = inflater.inflate(R.layout.activity_save, null)
        val btnSaveName: Button = popupSaveView.findViewById(R.id.btnSaveName)
        val etSaveName: EditText = popupSaveView.findViewById(R.id.etSaveName)
        val popupWindow = PopupWindow(
            popupSaveView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.showAtLocation(popupSaveView, Gravity.CENTER, 0, 0)

        btnSaveName.setOnClickListener {
            val name = etSaveName.text.toString()
            val story = chatService.createStorySnapshot(name)
            storyService.save(story)
            toast("$name Saved!")
        }
    }

    private fun createExternalFolder() {
        val folder = java.io.File(Environment.getExternalStorageDirectory(), "Nyagent")
        if (!folder.exists()) {
            folder.mkdir()
        }
    }

    private fun scrollToBottom() {
        if (chatService.messages.isNotEmpty()) {
            recyclerview.post { recyclerview.scrollToPosition(chatService.messages.size - 1) }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}
