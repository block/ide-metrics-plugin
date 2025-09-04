package com.squareup.eventstream

public interface Logger {
  public fun info(message: String)

  public fun error(
    message: String,
    throwable: Throwable
  )

  public companion object {
    public val NO_OP: Logger =
      object : Logger {
        override fun info(message: String) = Unit

        override fun error(
          message: String,
          throwable: Throwable
        ) = Unit
      }
  }
}
