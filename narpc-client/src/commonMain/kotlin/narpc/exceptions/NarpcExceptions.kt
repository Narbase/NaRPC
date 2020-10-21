package narpc.exceptions

class ServerException(val httpStatus: Int, val httpStatusDescription: String, message: String) :
    RuntimeException(message)

open class NarpcBaseException(val status: String, message: String) : RuntimeException(message)

class UnauthenticatedException(message: String) : NarpcBaseException(CommonCodes.UNAUTHENTICATED, message)
class InvalidRequestException(message: String) : NarpcBaseException(CommonCodes.INVALID_REQUEST, message)
class UnknownErrorException(message: String) : NarpcBaseException(CommonCodes.UNKNOWN_ERROR, message)


object CommonCodes {
    const val BASIC_SUCCESS = "0"
    const val UNAUTHENTICATED = "10"
    const val INVALID_REQUEST = "11"
    const val UNKNOWN_ERROR = "12"
    const val NOT_FOUND_ERROR = "13"
    const val OUTDATED_APP = "14"
    const val USER_DISABLED = "15"
}
