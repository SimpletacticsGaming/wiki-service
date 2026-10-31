package de.simpletactics.wiki.adapter

import io.kotest.core.spec.style.FunSpec
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("testing")
class FunSpecIT(
    private val jdbcTemplate: JdbcTemplate,
    body: FunSpecIT.() -> Unit = {}
) : FunSpec() {

    fun executeSql(path: String) {
        jdbcTemplate.execute(this::class.java.getResource("/scripts/$path")!!.readText())
    }

    init {
        afterTest {
            executeSql("clean_up.sql")
        }

        body()
    }
}
