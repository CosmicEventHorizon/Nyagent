<h1 align="center">🐱 Nyagent</h1>

<p align="center">
  <b>AI chat on Android, with local models, cloud models, and a built-in Linux workspace.</b>
</p>

<p align="center">
  <img src="https://github.com/CosmicEventHorizon/Nyagent/blob/main/images/nyagent.png" width="100" height="100">
</p>

<p align="center">
  <a href="https://github.com/CosmicEventHorizon/Nyagent"><img src="https://img.shields.io/github/stars/CosmicEventHorizon/Nyagent?style=social" alt="GitHub stars"></a>
  <a href="https://github.com/CosmicEventHorizon/Nyagent"><img src="https://img.shields.io/github/watchers/CosmicEventHorizon/Nyagent?style=social" alt="GitHub watchers"></a>
  <a href="https://github.com/CosmicEventHorizon/Nyagent/fork"><img src="https://img.shields.io/github/forks/CosmicEventHorizon/Nyagent?style=social" alt="GitHub forks"></a>
  <a href="https://github.com/CosmicEventHorizon/Nyagent"><img src="https://img.shields.io/github/last-commit/CosmicEventHorizon/Nyagent?color=red" alt="GitHub last commit"></a>
  <a href="./LICENSE"><img src="https://img.shields.io/badge/license-GPLv3-green.svg" alt="GPLv3"></a>
</p>

<div align="center">

## ⬇️ Download

### Stable

[![Stable Release](https://img.shields.io/badge/Nyagent%20Stable-latest-green)](https://github.com/CosmicEventHorizon/Nyagent/releases/latest)

### Experimental

Auto-generated from every commit on `main`.

[![Experimental](https://img.shields.io/badge/Nyagent%20Experimental-latest-orange)](https://github.com/CosmicEventHorizon/Nyagent/actions)

</div>

---

## 🚀 Getting Started

1. Grab the latest **stable** APK from [Releases](https://github.com/CosmicEventHorizon/Nyagent/releases/latest), or grab the [experimental build](https://github.com/CosmicEventHorizon/Nyagent/actions) from the latest run.
2. Open **Settings**.
3. Choose how you want to run AI:

   * **Ollama** — enter your Ollama server address and model.
   * **OpenRouter** — enter your API key and select a model.
4. Start chatting.

That's it.

---

## ✨ Features

### 🤖 AI that can actually do things

Nyagent gives supported models access to a private Linux workspace:

* Run shell commands and receive results
* Create, edit, read, and list files
* Download content over the web
* Write and run code
* Delegate large multi-step tasks to sub-agents

Everything runs inside the app's private environment.

### 🏠 Run local models with Ollama

Connect Nyagent to any Ollama server on your device, home network, or another machine — your models, your hardware, keep chats local.

### ☁️ Use hundreds of models with OpenRouter

Prefer cloud models? Add your OpenRouter API key, browse all available models, and pick one. You can also check your OpenRouter balance from Settings.

### 💬 Built for long conversations

* Clean messenger-style UI with color-coded message bubbles
* Edit an earlier message and continue from there
* Copy the text from any message
* Live context usage indicator
* Automatic context compaction for long chats
* Save and load conversations
* Stop a running generation whenever you want

### 🐧 Private Linux environment

Nyagent bundles a persistent Alpine Linux environment inside its private storage. It's installed on first launch (or from Settings) and gives the AI a real workspace—no Termux or external terminal app required.

---

## 🔧 Building from Source

Nyagent is written natively for Android in Kotlin.

```bash
# Debug APK
./gradlew app:buildDebug app:packageDebug app:assembleDebug
```

Output:

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
