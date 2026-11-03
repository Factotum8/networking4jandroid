import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseSigningTest {

    private val existing: (String) -> Boolean = { true }
    private val missing: (String) -> Boolean = { false }

    @Test
    fun `configured when path exists and password is set`() {
        val result = ReleaseSigning.resolve("/keys/upload.jks", "secret", existing)

        assertEquals(ReleaseSigning.Result.Configured("/keys/upload.jks", "secret"), result)
    }

    @Test
    fun `missing keystore path is reported`() {
        val result = ReleaseSigning.resolve(null, "secret", existing) as ReleaseSigning.Result.Missing

        assertEquals(1, result.problems.size)
        assertTrue(result.problems[0].contains(ReleaseSigning.KEYSTORE_ENV))
    }

    @Test
    fun `blank keystore path is reported as not set`() {
        val result = ReleaseSigning.resolve("  ", "secret", existing) as ReleaseSigning.Result.Missing

        assertTrue(result.problems.single().contains("is not set"))
    }

    @Test
    fun `nonexistent keystore file is reported`() {
        val result = ReleaseSigning.resolve("/keys/upload.jks", "secret", missing) as ReleaseSigning.Result.Missing

        assertTrue(result.problems.single().contains("missing file: /keys/upload.jks"))
    }

    @Test
    fun `missing password is reported`() {
        val result = ReleaseSigning.resolve("/keys/upload.jks", null, existing) as ReleaseSigning.Result.Missing

        assertTrue(result.problems.single().contains(ReleaseSigning.PASSWORD_ENV))
    }

    @Test
    fun `empty password is reported`() {
        val result = ReleaseSigning.resolve("/keys/upload.jks", "", existing) as ReleaseSigning.Result.Missing

        assertTrue(result.problems.single().contains("is not set"))
    }

    @Test
    fun `unresolved 1Password reference is reported`() {
        val result = ReleaseSigning.resolve(
            "/keys/upload.jks",
            "op://networking4j/networking4jandroid upload keystore/password",
            existing,
        ) as ReleaseSigning.Result.Missing

        assertTrue(result.problems.single().contains("op run"))
    }

    @Test
    fun `all problems are reported together`() {
        val result = ReleaseSigning.resolve(null, null, existing) as ReleaseSigning.Result.Missing

        assertEquals(2, result.problems.size)
    }

    @Test
    fun `file existence is not checked when path is not set`() {
        var checked = false
        ReleaseSigning.resolve(null, "secret") { checked = true; true }

        assertFalse(checked)
    }

    @Test
    fun `message lists every problem and points to the release script`() {
        val result = ReleaseSigning.resolve(null, null, existing) as ReleaseSigning.Result.Missing

        result.problems.forEach { assertTrue(result.message.contains(it)) }
        assertTrue(result.message.contains("scripts/bundle-release.sh"))
    }

    @Test
    fun `toString of configured result hides the password`() {
        val text = ReleaseSigning.Result.Configured("/keys/upload.jks", "secret").toString()

        assertFalse(text.contains("secret"))
        assertTrue(text.contains("/keys/upload.jks"))
    }
}
