/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.exception;

import java.sql.SQLException;
import javax.persistence.PersistenceException;

import org.hibernate.TransientObjectException;
import org.hibernate.TransientPropertyValueException;

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
		};
	}

	void onConstraintViolationOnSaveAndSaveOrUpdate(RuntimeException e);

	void onConstraintViolationOnPersistAndMergeAndFlush(RuntimeException e);

	void onTransientObjectOnSaveAndSaveOrUpdate(RuntimeException e);

	void onTransientObjectOnPersistAndMergeAndFlush(RuntimeException e);
}
