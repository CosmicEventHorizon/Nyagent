<h1 align="center">🐱 Nyagent</h1>

<p align="center">
  <b>AI chat on Android, with local models, cloud models, and a built-in Linux workspace.</b>
</p>

<p align="center">
  <img src="https://github.com/CosmicEventHorizon/Nyagent/blob/main/images/nyagent.png" width="100" height="100">
</p>

<p align="center">
  <a href="./LICENSE"><img src="https://img.shields.io/badge/license-GPLv3-green.svg" alt="GPLv3"></a>
  <a href="https://github.com/CosmicEventHorizon/Nyagent/releases"><img src="https://img.shields.io/github/v/release/CosmicEventHorizon/Nyagent?label=download" alt="Latest Release"></a>
</p>

---

## Nyagent

Nyagent is an open-source AI chat app for Android that works with both **local Ollama models** and **cloud models through OpenRouter**.

Unlike a basic chat frontend, Nyagent can also give supported models access to their own private Linux environment — letting them work with files, run commands, download resources, write code, and complete more involved tasks directly from your phone.

No desktop companion required.

## ✨ Features

### 🤖 AI that can actually do things

Nyagent gives supported models access to a private Linux workspace where they can:

* Run shell commands
* Create and edit files
* Read files and directories
* Download content from the web
* Write and run code
* Work through larger multi-step tasks

Everything happens inside the app's private environment.

### 🏠 Run local models with Ollama

Connect Nyagent to any Ollama server on your device, home network, or another machine.

Use your own models, your own hardware, and keep your chats local.

### ☁️ Use hundreds of models with OpenRouter

Prefer cloud models?

Add your OpenRouter API key and choose from the models available on OpenRouter directly inside Nyagent.

You can also view your OpenRouter balance from Settings.

### 💬 Built for long conversations

Nyagent includes the chat features you'd expect from a modern AI client:

* Clean messenger-style conversations
* Edit an earlier message and continue from there
* Copy text from any message
* See how much context is left
* Automatic context management for long chats
* Save and load conversations
* Stop generation whenever you want

### 🐧 Private Linux environment

Nyagent includes its own persistent Linux environment.

It lives entirely inside the app's private storage and is set up automatically, giving the AI a workspace without requiring Termux or another terminal app.

---

## 🚀 Getting Started

1. Download the latest APK from [Releases](https://github.com/CosmicEventHorizon/Nyagent/releases).
2. Open **Settings**.
3. Choose how you want to run AI:

   * **Ollama** — enter your Ollama server address and model.
   * **OpenRouter** — enter your API key and select a model.
4. Start chatting.

That's it.

---

## 🧠 Ollama + OpenRouter

Nyagent supports both local and cloud models through the same chat experience.

| Provider       | Best for                                        |
| -------------- | ----------------------------------------------- |
| **Ollama**     | Local models, privacy, offline setups           |
| **OpenRouter** | Easy access to cloud models from many providers |

Switch providers whenever you want.

---

## 🔧 Building from Source

Nyagent is built natively for Android with Kotlin.

```bash
# Compile
gradlew app:compileDebugKotlin

# Build debug APK
gradlew app:buildDebug app:packageDebug app:assembleDebug
```

The APK will be generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🤝 Contributing

Issues, bug reports, ideas, and pull requests are welcome.

If you find something broken or have an idea for Nyagent, open an issue on GitHub.

---

## 📄 License

Nyagent is free and open-source software released under the [GNU General Public License v3](LICENSE).
