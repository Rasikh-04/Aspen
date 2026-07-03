package app.aspen.server

import app.aspen.server.auth.AccountService
import app.aspen.server.auth.RecoveryMailer
import app.aspen.server.store.FileAccountRepository
import app.aspen.server.store.FileBlobRepository
import app.aspen.server.store.InMemoryAccountRepository
import app.aspen.server.store.InMemoryBlobRepository
import app.aspen.server.store.InMemoryRecoveryTokenRepository
import app.aspen.server.store.InMemorySessionRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.nio.file.Path

/**
 * Dev entry point. With no environment set this runs fully self-contained: in-memory stores,
 * fake model provider, no network egress, no credential anywhere — the testable-without-API
 * mode. Deployment configuration (real provider, durable dir, mail delivery) is env-only; see
 * [ServerConfig]. Hosting itself is a deferred decision (docs/07 Phase 6.9).
 */
fun main() {
    val config = ServerConfig.fromEnv()
    val deps = buildDeps(config)

    embeddedServer(Netty, port = config.port) { aspenServer(deps) }
        .start(wait = true)
}

internal fun buildDeps(config: ServerConfig): ServerDeps {
    val dataDir = config.dataDir?.let { Path.of(it) }
    val accounts = dataDir?.let(::FileAccountRepository) ?: InMemoryAccountRepository()
    val blobs = dataDir?.let(::FileBlobRepository) ?: InMemoryBlobRepository()
    val sessions = InMemorySessionRepository()
    val recovery = InMemoryRecoveryTokenRepository()

    // Dev-only delivery: the recovery token appears on the server console, never in any file.
    // A real mail sender is deployment configuration (docs/07 Phase 6.9).
    val mailer = RecoveryMailer { email, token ->
        println("[aspen-server] recovery token for $email: $token (valid 30 min)")
    }

    return ServerDeps(
        service = AccountService(accounts, sessions, recovery, blobs, mailer),
        blobs = blobs,
        provider = buildModelProvider(config, HttpClient(CIO)),
    )
}
