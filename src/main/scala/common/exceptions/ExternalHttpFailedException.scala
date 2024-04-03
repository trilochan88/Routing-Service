package com.ts
package common.exceptions

class ExternalHttpFailedException(message: String,throwable: Throwable) extends Exception(message, throwable) {}
