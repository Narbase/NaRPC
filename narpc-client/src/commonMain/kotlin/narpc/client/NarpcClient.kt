package narpc.client

expect object NarpcClient {
    inline fun <reified T : Any> build(endpoint: String): T
}