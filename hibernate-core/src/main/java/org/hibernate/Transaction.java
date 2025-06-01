/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.EntityTransaction;
import jakarta.transaction.Synchronization;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.resource.transaction.spi.TransactionStatus;

import java.util.function.Consumer;

import static org.hibernate.resource.transaction.backend.jta.internal.StatusTranslator.translate;

/**
 * Represents a resource-local transaction, where <em>resource-local</em> is interpreted
 * by Hibernate to mean any transaction under the control of Hibernate. That is to say,
 * the underlying transaction might be a JTA transaction, or it might be a JDBC transaction,
 * depending on how Hibernate is configured.
 * <p>
 * Every resource-local transaction is associated with a {@link Session} and begins with
 * an explicit call to {@link Session#beginTransaction()}, or, almost equivalently, with
 * {@code session.getTransaction().begin()}, and ends with a call to {@link #commit()}
 * or {@link #rollback()}.
 * <p>
 * When a transaction rolls back, Hibernate makes no attempt to roll back the state of
 * entity instances held in memory to their state at the beginning of the transaction.
 * After a transaction rollback, the current {@linkplain Session persistence context}
 * must be discarded, and the state of its entities must be assumed inconsistent with
 * the state held by the database.
 * <p>
 * A single session might span multiple transactions since the notion of a session
 * (a conversation between the application and the datastore) is of coarser granularity
 * than the concept of a database transaction. However, there is at most one uncommitted
 * transaction associated with a given {@link Session} at any time.
 * <p>
 * Note that this interface is never used to control container managed JTA transactions,
 * and is not usually used to control transactions that affect multiple resources.
 * <p>
 * A {@code Transaction} object is not threadsafe.
 *
 * @apiNote JPA doesn't allow an {@link EntityTransaction} to represent a JTA transaction.
 * But when {@linkplain org.hibernate.jpa.spi.JpaCompliance#isJpaTransactionComplianceEnabled
 * strict JPA transaction compliance} is disabled, as it is by default, Hibernate allows an
 * instance of this interface to represent the current JTA transaction context.
 *
 * @author Anton van Straaten
 * @author Steve Ebersole
 * @author Gavin King
 *
 * @see Session#beginTransaction()
 */
public interface Transaction extends EntityTransaction {
	/**
	 * Get the current {@linkplain TransactionStatus status} of this transaction.
	 *
	 * @apiNote {@link TransactionStatus} belongs to an SPI package, and so this
	 *          operation is a (fairly harmless) layer-breaker. Prefer the use
	 *          of {@link #isActive}, {@link #isComplete}, {@link #wasStarted},
	 *          {@link #wasSuccessful}, {@link #wasFailure}, or
	 *          {@link #isInCompletionProcess} according to need.
	 *
	 */
	TransactionStatus getStatus();

	/**
	 * Is this transaction still active?
	 * <p>
	 * A transaction which has been {@linkplain #markRollbackOnly marked for rollback}
	 * is still considered active, and is still able to perform work. To determine if
	 * a transaction has been marked for rollback, call {@link #getRollbackOnly()}.
	 *
	 * @return {@code true} if the {@linkplain #getStatus status}
	 *         is {@link TransactionStatus#ACTIVE} or
	 *         {@link TransactionStatus#MARKED_ROLLBACK}
	 */
	@Override
	default boolean isActive() {
		return switch (getStatus()) {
			case ACTIVE, MARKED_ROLLBACK -> true;
			default -> false;
		};
	}

	/**
	 * Is this transaction currently in the completion process?
	 * <p>
	 * Note that a {@link Synchronization} is called <em>before</em> and <em>after</em>
	 * the completion process. Therefore, this state is not usually observable to the
	 * client program logic.
	 *
	 * @return {@code true} if the {@linkplain #getStatus status}
	 *         is {@link TransactionStatus#COMMITTING} or
	 *         {@link TransactionStatus#ROLLING_BACK}
	 *
	 * @since 7.0
	 */
	@Incubating
	default boolean isInCompletionProcess() {
		return switch (getStatus()) {
			case COMMITTING, ROLLING_BACK -> true;
			default -> false;
		};
	}

	/**
	 * Is this transaction complete?
	 *
	 * @return {@code true} if the {@linkplain #getStatus status}
	 *         is {@link TransactionStatus#COMMITTED},
	 *         {@link TransactionStatus#ROLLED_BACK},
	 *         {@link TransactionStatus#FAILED_COMMIT}, or
	 *         {@link TransactionStatus#FAILED_ROLLBACK}
	 *
	 * @since 7.0
	 */
	@Incubating
	default boolean isComplete() {
		return switch (getStatus()) {
			case COMMITTED, ROLLED_BACK, FAILED_COMMIT, FAILED_ROLLBACK -> true;
			default -> false;
		};
	}

	/**
	 * Was this transaction already started?
	 *
	 * @return {@code true} if the {@linkplain #getStatus status} is
	 *         anything other than {@link TransactionStatus#NOT_ACTIVE}
	 *
	 * @since 7.0
	 */
	@Incubating
	default boolean wasStarted() {
		return getStatus() != TransactionStatus.NOT_ACTIVE;
	}

	/**
	 * Was this transaction already successfully committed?
	 *
	 * @return {@code true} if the {@linkplain #getStatus status}
	 *         is {@link TransactionStatus#COMMITTED}
	 *
	 * @since 7.0
	 */
	@Incubating
	default boolean wasSuccessful() {
		return getStatus() == TransactionStatus.COMMITTED;
	}

	/**
	 * Was this transaction a failure? Here we consider a successful rollback,
	 * a failed commit, or a failed rollback to amount to transaction failure.
	 *
	 * @return {@code true} if the {@linkplain #getStatus status}
	 *         is {@link TransactionStatus#ROLLED_BACK},
	 *         {@link TransactionStatus#FAILED_COMMIT},
	 *         {@link TransactionStatus#FAILED_ROLLBACK}
	 *
	 * @since 7.0
	 */
	@Incubating
	default boolean wasFailure() {
		return switch (getStatus()) {
			case ROLLED_BACK, FAILED_COMMIT, FAILED_ROLLBACK -> true;
			default -> false;
		};
	}

	/**
	 * Register an action which will be called during the "before completion" phase.
	 *
	 * @since 7.0
	 */
	@Incubating
	default void runBeforeCompletion(Runnable action) {
		registerSynchronization( new Synchronization() {
			@Override
			public void beforeCompletion() {
				action.run();
			}
			@Override
			public void afterCompletion(int status) {
			}
		} );
	}

	/**
	 * Register an action which will be called during the "after completion" phase.
	 *
	 * @since 7.0
	 */
	@Incubating
	default void runAfterCompletion(Consumer<TransactionStatus> action) {
		registerSynchronization( new Synchronization() {
			@Override
			public void beforeCompletion() {
			}
			@Override
			public void afterCompletion(int status) {
				action.accept( translate( status ) );
			}
		} );
	}

	/**
	 * Register a {@linkplain Synchronization synchronization callback} for this transaction.
	 *
	 * @param synchronization The {@link Synchronization} callback to register
	 *
	 * @apiNote {@link Synchronization} is a type defined by JTA, but this operation does
	 *          not depend on the use of JTA for transaction management. Prefer the use of
	 *          the methods {@link #runBeforeCompletion} and {@link #runAfterCompletion}
	 *          for convenience.
	 */
	void registerSynchronization(Synchronization synchronization);

	/**
	 * Set the transaction timeout for any transaction started by a subsequent call to
	 * {@link #begin} on this instance of {@code Transaction}.
	 *
	 * @param seconds The number of seconds before a timeout
	 */
	void setTimeout(int seconds);

	/**
	 * Retrieve the transaction timeout set for this instance.
	 * <p>
	 * A {@code null} return value indicates that no timeout has been set.
	 *
	 * @return the timeout, in seconds, or {@code null}
	 */
	@Override
	@Nullable Integer getTimeout();

	/**
	 * Attempt to mark the underlying transaction for rollback only.
	 * <p>
	 * Unlike {@link #setRollbackOnly()}, which is specified by JPA
	 * to throw when the transaction is inactive, this operation may
	 * be called on an inactive transaction, in which case it has no
	 * effect.
	 *
	 * @see #setRollbackOnly()
	 */
	void markRollbackOnly();
}
