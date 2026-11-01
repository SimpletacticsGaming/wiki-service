package de.simpletactics.wiki.adapter

import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

/**
 * Each test runs in a transaction that is rolled back afterwards, so fixtures
 * loaded with [executeSql] need no counterpart to delete them again - same setup
 * as sita-backend's `FunSpecIT`.
 *
 * [SpringExtension] has to be registered by hand for that: classpath autoscan
 * only picks up the constructor-autowiring extension, and without the test
 * extension Spring's `TransactionalTestExecutionListener` is never invoked
 * around a test, so nothing rolls back. It also means the rollback depends on
 * Kotest running a spec's tests sequentially - the transaction is bound to the
 * thread, so enabling test concurrency would break it.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("testing")
@Transactional
class FunSpecIT(
    private val jdbcTemplate: JdbcTemplate,
    body: FunSpecIT.() -> Unit = {}
) : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    fun executeSql(path: String) {
        jdbcTemplate.execute(this::class.java.getResource("/scripts/$path")!!.readText())
    }

    init {
        body()
    }
}
