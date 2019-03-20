/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.exceptionhandling;

import java.sql.SQLException;
import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;

import org.hibernate.StaleObjectStateException;
import org.hibernate.TransactionException;
import org.hibernate.TransientObjectException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.hql.internal.ast.QuerySyntaxException;
import org.hibernate.id.IdentifierGenerationException;

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
				assertThat( e.getCause(), instanceOf( ConstraintViolationException.class ) );
				assertThat( e.getCause().getCause(), instanceOf( SQLException.class ) );
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
				assertThat( e.getCause(), instanceOf( QuerySyntaxException.class ) );
			}

			@Override
			public void onGetSingleResultWithMultipleResults(RuntimeException e) {
				assertThat( e, instanceOf( javax.persistence.NonUniqueResultException.class ) );
			}

			@Override
			public void onGetSingleResultWithNoResults(RuntimeException e) {
				assertThat( e, instanceOf( NoResultException.class ) );
			}

			@Override
			public void onStaleObjectMergeAndUpdateFlush(RuntimeException e) {
				assertThat( e, instanceOf( OptimisticLockException.class ) );
				assertThat( e.getCause(), instanceOf( StaleObjectStateException.class ) );
			}

			@Override
			public void onIdentifierGeneratorFailure(RuntimeException e) {
				assertThat( e, instanceOf( PersistenceException.class ) );
				assertThat( e.getCause(), instanceOf( IdentifierGenerationException.class ) );
			}

			@Override
			public void onTransactionExceptionOnSaveAndSaveOrUpdate(RuntimeException e) {
				assertThat( e, instanceOf( TransactionException.class ) );
			}

			@Override
			public void onTransactionExceptionOnPersistAndMergeAndFlush(RuntimeException e) {
				assertThat( e, instanceOf( PersistenceException.class ) );
				assertThat( e.getCause(), instanceOf( TransactionException.class ) );
			}

			@Override
			public void onTransactionExceptionOnCommit(RuntimeException e) {
				assertThat( e, instanceOf( RollbackException.class ) );
				assertThat( e.getCause(), instanceOf( PersistenceException.class ) );
				assertThat( e.getCause().getCause(), instanceOf( TransactionException.class ) );
			}

			@Override
			public void onExecuteUpdateWithConstraintViolation(RuntimeException e) {
				assertThat( e, instanceOf( PersistenceException.class ) );
				assertThat( e.getCause(), instanceOf( ConstraintViolationException.class ) );
				assertThat( e.getCause().getCause(), instanceOf( SQLException.class ) );
			}
		};
	}

	static ExceptionExpectations nativePre52() {
		return new ExceptionExpectations() {
			@Override
			public void onConstraintViolationOnSaveAndSaveOrUpdate(RuntimeException e) {
				assertThat( e, instanceOf( ConstraintViolationException.class ) );
				assertThat( e.getCause(), instanceOf( SQLException.class ) );
			}

			@Override
			public void onConstraintViolationOnPersistAndMergeAndFlush(RuntimeException e) {
				assertThat( e, instanceOf( ConstraintViolationException.class ) );
				assertThat( e.getCause(), instanceOf( SQLException.class ) );
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
				assertThat( e, instanceOf( QuerySyntaxException.class ) );
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
				assertThat( e, instanceOf( StaleObjectStateException.class ) );
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
				assertThat( e.getCause(), instanceOf( ConstraintViolationException.class ) );
				assertThat( e.getCause().getCause(), instanceOf( SQLException.class ) );
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
				assertThat( e.getCause(), instanceOf( QuerySyntaxException.class ) );
			}

			@Override
			public void onGetSingleResultWithMultipleResults(RuntimeException e) {
				assertThat( e, instanceOf( javax.persistence.NonUniqueResultException.class ) );
			}

			@Override
			public void onGetSingleResultWithNoResults(RuntimeException e) {
				assertThat( e, instanceOf( NoResultException.class ) );
			}

			@Override
			public void onStaleObjectMergeAndUpdateFlush(RuntimeException e) {
				assertThat( e, instanceOf( OptimisticLockException.class ) );
				assertThat( e.getCause(), instanceOf( StaleObjectStateException.class ) );
			}

			@Override
			public void onIdentifierGeneratorFailure(RuntimeException e) {
				assertThat( e, instanceOf( PersistenceException.class ) );
				assertThat( e.getCause(), instanceOf( IdentifierGenerationException.class ) );
			}

			@Override
			public void onTransactionExceptionOnSaveAndSaveOrUpdate(RuntimeException e) {
				assertThat( e, instanceOf( TransactionException.class ) );
			}

			@Override
			public void onTransactionExceptionOnPersistAndMergeAndFlush(RuntimeException e) {
				assertThat( e, instanceOf( PersistenceException.class ) );
				assertThat( e.getCause(), instanceOf( TransactionException.class ) );
			}

			@Override
			public void onTransactionExceptionOnCommit(RuntimeException e) {
				assertThat( e, instanceOf( PersistenceException.class ) );
				assertThat( e.getCause(), instanceOf( TransactionException.class ) );
			}

			@Override
			public void onExecuteUpdateWithConstraintViolation(RuntimeException e) {
				assertThat( e, instanceOf( PersistenceException.class ) );
				assertThat( e.getCause(), instanceOf( ConstraintViolationException.class ) );
				assertThat( e.getCause().getCause(), instanceOf( SQLException.class ) );
			}
		};
	}

	void onConstraintViolationOnSaveAndSaveOrUpdate(RuntimeException e);

	void onConstraintViolationOnPersistAndMergeAndFlush(RuntimeException e);

	void onTransientObjectOnSaveAndSaveOrUpdate(RuntimeException e);

	void onTransientObjectOnPersistAndMergeAndFlush(RuntimeException e);

	void onInvalidQueryExecuted(RuntimeException e);

	void onGetSingleResultWithMultipleResults(RuntimeException e);

	void onGetSingleResultWithNoResults(RuntimeException e);

	void onStaleObjectMergeAndUpdateFlush(RuntimeException e);

	void onIdentifierGeneratorFailure(RuntimeException e);

	void onTransactionExceptionOnSaveAndSaveOrUpdate(RuntimeException e);

	void onTransactionExceptionOnPersistAndMergeAndFlush(RuntimeException e);

	void onTransactionExceptionOnCommit(RuntimeException e);

	void onExecuteUpdateWithConstraintViolation(RuntimeException e);
}