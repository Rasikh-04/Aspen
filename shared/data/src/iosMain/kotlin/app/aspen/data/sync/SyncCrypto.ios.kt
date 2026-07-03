package app.aspen.data.sync

/**
 * iOS: NO real passphrase crypto yet (CryptoKit/CommonCrypto actual pending, same placeholder
 * policy as the local cipher — docs/STATUS.md, PRE_SHIP §3). Returning null DISABLES backup on
 * iOS entirely: a passthrough here would upload readable content, which is never acceptable.
 */
actual fun platformSyncCrypto(): SyncCrypto? = null
