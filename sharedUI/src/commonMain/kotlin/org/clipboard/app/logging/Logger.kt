package org.clipboard.app.logging

enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR }

object Redactor {
	fun redact(value: String?, replacement: String = "***"): String = when {
		value == null -> "<null>"
		value.isBlank() -> "<blank>"
		value.length <= 6 -> replacement
		else -> value.take(2) + replacement + value.takeLast(2)
	}
}

object Logger {
	var minLevel: LogLevel = LogLevel.DEBUG

	private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
		if (level.ordinal < minLevel.ordinal) return
		val prefix = when (level) {
			LogLevel.VERBOSE -> "🔍"
			LogLevel.DEBUG -> "🐛"
			LogLevel.INFO -> "ℹ️"
			LogLevel.WARN -> "⚠️"
			LogLevel.ERROR -> "❌"
		}
		val text = "$prefix [$tag] $message"
		println(text)
		throwable?.let { println(it.stackTraceToString()) }
	}

	fun v(tag: String, message: String) = log(LogLevel.VERBOSE, tag, message)
	fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
	fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
	fun w(tag: String, message: String, t: Throwable? = null) = log(LogLevel.WARN, tag, message, t)
	fun e(tag: String, message: String, t: Throwable? = null) = log(LogLevel.ERROR, tag, message, t)
}



