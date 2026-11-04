package de.simpletactics.wiki.adapter

import org.springframework.transaction.annotation.Transactional

/**
 * Rolls back on any exception, not just unchecked ones, so a half-applied wiki
 * tree mutation can never be committed.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Transactional(rollbackFor = [Exception::class])
annotation class SitaTransactional
