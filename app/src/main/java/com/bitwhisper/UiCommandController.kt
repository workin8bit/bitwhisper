package com.bitwhisper

class UiCommandController(private val executor: UiActionExecutor) {
    private val confirmations = ConfirmationManager()

    fun click(label: String): String {
        if (ActionSafety.risk(label) == ActionRisk.IMPORTANT) return confirmations.request("tekan tombol $label")
        return if (executor.click(label)) "Tombol $label ditekan." else "Tombol $label tidak ditemukan."
    }

    fun confirm(): String {
        val command = confirmations.confirm() ?: return "Tidak ada aksi yang menunggu konfirmasi."
        val label = command.removePrefix("tekan tombol ")
        return if (executor.clickConfirmed(label)) "Tombol $label ditekan." else "Tombol $label tidak ditemukan."
    }

    fun cancel(): String { confirmations.cancel(); return "Aksi UI dibatalkan." }
}
