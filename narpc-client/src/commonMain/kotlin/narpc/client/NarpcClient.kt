package narpc.client

expect object NarpcClient {
    inline fun <reified T : Any> build(endpoint: String, headers: Map<String, String> = mapOf()): T
}