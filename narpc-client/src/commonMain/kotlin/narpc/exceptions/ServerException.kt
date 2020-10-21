package narpc.exceptions

class ServerException(val httpStatus: Int, val httpStatusDescription: String, message: String) : RuntimeException(message)