/**
 * Release (upload key) signing settings, resolved from environment variables
 * so no secret is ever stored in the repo. The password comes from the
 * 1Password vault `networking4j` via `op run` (see scripts/bundle-release.sh).
 */
object ReleaseSigning {
    const val KEYSTORE_ENV = "ANDROID_UPLOAD_KEYSTORE"
    const val PASSWORD_ENV = "ANDROID_UPLOAD_KEYSTORE_PASSWORD"
    const val KEY_ALIAS = "upload"

    sealed interface Result {
        /** PKCS12 keystore: the same password protects the store and the key. */
        data class Configured(val keystorePath: String, val password: String) : Result {
            // Never let the password leak into logs or exception messages.
            override fun toString() = "Configured(keystorePath=$keystorePath, password=***)"
        }

        data class Missing(val problems: List<String>) : Result {
            val message: String
                get() = buildString {
                    appendLine("Release signing is not configured:")
                    problems.forEach { appendLine("  - $it") }
                    append("Build release artifacts with scripts/bundle-release.sh ")
                    append("(injects the upload keystore password from 1Password via `op run`).")
                }
        }
    }

    /**
     * Pure resolution step: reads only the given values, so it can be unit-tested
     * without touching the real environment or file system.
     */
    fun resolve(
        keystorePath: String?,
        password: String?,
        fileExists: (String) -> Boolean,
    ): Result {
        val problems = buildList {
            if (keystorePath.isNullOrBlank()) {
                add("$KEYSTORE_ENV is not set (path to the upload .jks)")
            } else if (!fileExists(keystorePath)) {
                add("$KEYSTORE_ENV points to a missing file: $keystorePath")
            }
            if (password.isNullOrEmpty()) {
                add("$PASSWORD_ENV is not set")
            } else if (password.startsWith("op://")) {
                add("$PASSWORD_ENV holds an unresolved 1Password reference; run the build under `op run`")
            }
        }
        return if (problems.isEmpty()) {
            Result.Configured(keystorePath!!, password!!)
        } else {
            Result.Missing(problems)
        }
    }
}
