package com.bitwhisper

object ModelLinks {
    // Tiny is much faster on-device; base remains available as a fallback.
    const val WHISPER = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin?download=true"
    const val CHAT = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf?download=true"
}
