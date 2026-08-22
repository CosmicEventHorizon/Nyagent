package com.pirouette.nyagent.application.prompts

/**
 * Instructs the model to summarise a long conversation into a compact form that
 * preserves every instruction, constraint, decision, and result needed to
 * continue the task without the full history.
 */
const val COMPACTION_SYSTEM_PROMPT = """
You are a conversation compactor.

Your job is to compress the conversation below into a concise summary that preserves:
- the user's original request and any constraints;
- every instruction given to or by agents;
- the purpose and outcome of each tool call;
- key facts, decisions, errors, and results needed to continue.

Keep it dense but complete. Do not add new information. Return ONLY the summary text.
"""
