/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exceptionhandling;

import java.sql.SQLException;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.RollbackException;

import org.hibernate.StaleStateException;
import org.hibernate.TransactionException;
import org.hibernate.TransientObjectException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.query.SemanticException;
import org.hibernate.query.SyntaxException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

interface ExceptionExpectations {

	static ExceptionExpectations jpa() {
		return new ExceptionExpectations() {
			@Override
			public void onConstraintViolationOnSaveAndSaveOrUpdate(RuntimeException e) {
				assertThat( e, instanceOf( PersistenceException.class ) );
			}

			@Override
			public void onConstraintViolationOnPersistAndMergeAndFlush(RuntimeException e) {
				assertThat( e, instanceOf( PersistenceException.class ) );
				assertThat( e, instanceOf( ConstraintViolationException.class ) );
				assertThat( e.getCause(), instanceOf( SQLException.class ) );
			}

			@Override
			public void onTransientObjectOnSaveAndSaveOrUpdate(RuntimeException e) {
				assertThat( e, instanceOf( TransientObjectException.class ) );
			}

			@Override
			public void onTransientObjectOnPersistAndMergeAndFlush(RuntimeException e) {
				assertThat( e, instanceOf( IllegalStateException.class ) );
				assertThat( e.getCause(), instanceOf( TransientObjectException.class ) );
			}

			@Override
			public void onInvalidQueryExecuted(RuntimeException e) {
				assertThat( e, instanceOf( IllegalArgumentException.class ) );
				assertThat( e.getCause(), instanceOf( SyntaxException.class ) );
			}

			@Override
			public void onUniqueResultWithMultipleResults(RuntimeException e) {
				assertThat( e, instanceOf( org.hibernate.NonUniqueResultException.class ) );
			}

			@Override
			public void onGetSingleResultWithMultipleResults(RuntimeException e) {
				assertThat( e, instanceOf( jakarta.persistence.NonUniqueResultException.class ) );
			}

			@Override
			public void onGetSingleResultWithNoResults(RuntimeException e) {
				assertThat( e, instanceOf( NoResultException.class ) );
			}

			@Override
			public void onStaleObjectMergeAndUpdateFlush(RuntimeException e) {
				assertThat( e, instanceOf( OptimisticLockException.class ) );
				assertThat( e.getCause(), instanceOf( StaleStateException.class ) );
			}

			@Override
			public void onIdentifierGeneratorFailure(RuntimeException e) {
				assertThat( e, instanceOf( PersistenceException.class ) );
				assertThat( e, instanceOf( IdentifierGenerationException.class ) );
			}

			@Override
			public void onTransactionExceptionOnSaveAndSaveOrUpdate(RuntimeException e) {
				assertThat( e, instanceOf( TransactionException.class ) );
			}

			@Override
			public void onTransactionExceptionOnPersistAndMergeAndFlush(RuntimeException e) {
				assertThat( e, instanceOf( PersistenceException.class ) );
				assertThat( e, instanceOf( TransactionException.class ) );
			}

			@Override
			public void onTransactionExceptionOnCommit(RuntimeException e) {
				assertThat( e, instanceOf( RollbackException.class ) );
				assertThat( e, instanceOf( PersistenceException.class ) );
				assertThat( e.getCause(), instanceOf( TransactionException.class ) );
			}

			@Override
			public void onExecuteUpdateWithConstraintViolation(RuntimeException e) {
				assertThat( e, instanceOf( PersistenceException.class ) );
				assertThat( e, instanceOf( ConstraintViolationException.class ) );
				assertThat( e.getCause(), instanceOf( SQLException.class ) );
			}
		};
	}

	static ExceptionExpectations nativePre52() {
		return new ExceptionExpectations() {
			@Override
			public void onConstraintViolationOnSaveAndSaveOrUpdate(RuntimeException e) {
				assertThat( e, instanceOf( ConstraintViolationException.class ) );
				assertThat( e, instanceOf( SQLException.class ) );
			}

			@Override
			public void onConstraintViolationOnPersistAndMergeAndFlush(RuntimeException e) {
				assertThat( e, instanceOf( ConstraintViolationException.class ) );
				assertThat( e, instanceOf( SQLException.class ) );
			}

			@Override
			public void onTransientObjectOnSaveAndSaveOrUpdate(RuntimeException e) {
				assertThat( e, instanceOf( TransientObjectException.class ) );
			}

			@Override
			public void onTransientObjectOnPersistAndMergeAndFlush(RuntimeException e) {
				assertThat( e, instanceOf( TransientObjectException.class ) );
			}

			@Override
			public void onInvalidQueryExecuted(RuntimeException e) {
				assertThat( e, instanceOf( SemanticException.class ) );
			}

			@Override
			public void onUniqueResultWithMultipleResults(RuntimeException e) {
				assertThat( e, instanceOf( org.hibernate.NonUniqueResultException.class ) );
			}

			@Override
			public void onGetSingleResultWithMultipleResults(RuntimeException e) {
				assertThat( e, instanceOf( org.hibernate.NonUniqueResultException.class ) );
			}

			@Override
			public void onGetSingleResultWithNoResults(RuntimeException e) {
				assertThat( e, instanceOf( NoResultException.class ) );
			}

			@Override
			public void onStaleObjectMergeAndUpdateFlush(RuntimeException e) {
				assertThat( e, instanceOf( StaleStateException.class ) );
			}

			@Override
			public void onIdentifierGeneratorFailure(RuntimeException e) {
				assertThat( e, instanceOf( IdentifierGenerationException.class ) );
			}

			@Override
			public void onTransactionExceptionOnSaveAndSaveOrUpdate(RuntimeException e) {
				assertThat( e, instanceOf( TransactionException.class ) );
			}

			@Override
			public void onTransactionExceptionOnPersistAndMergeAndFlush(RuntimeException e) {
				assertThat( e, instanceOf( TransactionException.class ) );
			}

			@Override
			public void onTransactionExceptionOnCommit(RuntimeException e) {
				assertThat( e, instanceOf( TransactionException.class ) );
			}

			@Override
			public void onExecuteUpdateWithConstraintViolation(RuntimeException e) {
				assertThat( e, instanceOf( ConstraintViolationException.class ) );
				assertThat( e.getCause(), instanceOf( SQLException.class ) );
			}
		};
	}

	static ExceptionExpectations nativePost52() {
		return new ExceptionExpectations() {
			@Override
			public void onConstraintViolationOnSaveAndSaveOrUpdate(RuntimeException e) {
				assertThat( e, instanceOf( ConstraintViolationException.class ) );
				assertThat( e.getCause(), instanceOf( SQLException.class ) );
			}

			@Override
			public void onConstraintViolationOnPersistAndMergeAndFlush(RuntimeException e) {
				assertThat( e, instanceOf( PersistenceException.class ) );
				assertThat( e, instanceOf( ConstraintViolationException.class ) );
				assertThat( e.getCause(), instanceOf( SQLException.class ) );
			}

			@Override
			public void onTransientObjectOnSaveAndSaveOrUpdate(RuntimeException e) {
				assertThat( e, instanceOf( TransientObjectException.class ) );
			}

			@Override
			public void onTransientObjectOnPersistAndMergeAndFlush(RuntimeException e) {
				assertThat( e, instanceOf( IllegalStateException.class ) );
				assertThat( e.getCause(), instanceOf( TransientObjectException.class ) );
			}

			@Override
			public void onInvalidQueryExecuted(RuntimeException e) {
				assertThat( e, instanceOf( IllegalArgumentException.class ) );
				assertThat( e.getCause(), instanceOf( SyntaxException.class ) );
			}

			@Override
			public void onUniqueResultWithMultipleResults(RuntimeException e) {
				assertThat( e, instanceOf( org.hibernate.NonUniqueResultException.class ) );
			}

			@Override
			public void onGetSingleResultWithMultipleResults(RuntimeException e) {
				assertThat( e, instanceOf( jakarta.persistence.NonUniqueResultException.class ) );
			}

			@Override
			public void onGetSingleResultWithNoResults(RuntimeException e) {
				assertThat( e, instanceOf( NoResultException.class ) );
			}

			@Override
			public void onStaleObjectMergeAndUpdateFlush(RuntimeException e) {
				assertThat( e, instanceOf( OptimisticLockException.class ) );
				assertThat( e.getCause(), instanceOf( StaleStateException.class ) );
			}

			@Override
			public void onIdentifierGeneratorFailure(RuntimeException e) {
				assertThat( e, instanceOf( PersistenceException.class ) );
				assertThat( e, instanceOf( IdentifierGenerationException.class ) );
			}

			@Override
			public void onTransactionExceptionOnSaveAndSaveOrUpdate(RuntimeException e) {
				assertThat( e, instanceOf( TransactionException.class ) );
			}

			@Override
			public void onTransactionExceptionOnPersistAndMergeAndFlush(RuntimeException e) {
				assertThat( e, instanceOf( PersistenceException.class ) );
				assertThat( e, instanceOf( TransactionException.class ) );
			}

			@Override
			public void onTransactionExceptionOnCommit(RuntimeException e) {
				assertThat( e, instanceOf( PersistenceException.class ) );
				assertThat( e, instanceOf( TransactionException.class ) );
			}

			@Override
			public void onExecuteUpdateWithConstraintViolation(RuntimeException e) {
				assertThat( e, instanceOf( PersistenceException.class ) );
				assertThat( e, instanceOf( ConstraintViolationException.class ) );
				assertThat( e.getCause(), instanceOf( SQLException.class ) );
			}
		};
	}

	void onConstraintViolationOnSaveAndSaveOrUpdate(RuntimeException e);

	void onConstraintViolationOnPersistAndMergeAndFlush(RuntimeException e);

	void onTransientObjectOnSaveAndSaveOrUpdate(RuntimeException e);

	void onTransientObjectOnPersistAndMergeAndFlush(RuntimeException e);

	void onInvalidQueryExecuted(RuntimeException e);

	void onUniqueResultWithMultipleResults(RuntimeException e);

	void onGetSingleResultWithMultipleResults(RuntimeException e);

	void onGetSingleResultWithNoResults(RuntimeException e);

	void onStaleObjectMergeAndUpdateFlush(RuntimeException e);

	void onIdentifierGeneratorFailure(RuntimeException e);

	void onTransactionExceptionOnSaveAndSaveOrUpdate(RuntimeException e);

	void onTransactionExceptionOnPersistAndMergeAndFlush(RuntimeException e);

	void onTransactionExceptionOnCommit(RuntimeException e);

	void onExecuteUpdateWithConstraintViolation(RuntimeException e);
}
