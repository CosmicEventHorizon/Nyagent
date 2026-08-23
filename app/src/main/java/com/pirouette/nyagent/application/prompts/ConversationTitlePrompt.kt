package com.pirouette.nyagent.application.prompts

/** Instructions for the lightweight, tool-free conversation title request. */
const val CONVERSATION_TITLE_SYSTEM_PROMPT = """
Create a short title for a conversation that begins with the user's message below.

Use 3 to 7 words. Return ONLY the title: no quotes, no label, no explanation, and no ending punctuation.
"""
