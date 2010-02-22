/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb;

import java.util.List;
import java.util.Map;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.Selection;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.StaleStateException;
import org.hibernate.ejb.criteria.ValueHandlerFactory;
import org.hibernate.type.Type;

/**
 * Additional internal contracts for the Hibernate {@link javax.persistence.EntityManager} implementation.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public interface HibernateEntityManagerImplementor extends HibernateEntityManager {
	/**
	 * Get access to the Hibernate extended EMF contract.
	 *
	 * @return The Hibernate EMF contract for this EM.
	 */
	public HibernateEntityManagerFactory getFactory();

	/**
	 * Provides access to whether a transaction is currently in progress.
	 *
	 * @return True if a transaction is considered currently in progress; false otherwise.
	 */
	boolean isTransactionInProgress();

	/**
	 * Handles marking for rollback and other such operations that need to occur depending on the type of
	 * exception being handled.
	 *
	 * @param e The exception being handled.
	 */
	public void handlePersistenceException(PersistenceException e);

	/**
	 * Delegates to {@link #handlePersistenceException} and then throws the given exception.
	 *
	 * @param e The exception being handled and finally thrown.
	 */
	public void throwPersistenceException(PersistenceException e);

	/**
	 * Converts a Hibernate-specific exception into a JPA-specified exception; note that the JPA sepcification makes use
	 * of exceptions outside its exception hierarchy, though they are all runtime exceptions.
	 * <p/>
	 * Any appropriate/needed calls to {@link #handlePersistenceException} are also made.
	 *
	 * @param e The Hibernate excepton.
	 * @param lockOptions The lock options in effect at the time of exception (can be null)
	 *
	 * @return The JPA-specified exception
	 */
	public RuntimeException convert(HibernateException e, LockOptions lockOptions);

	/**
	 * Converts a Hibernate-specific exception into a JPA-specified exception; note that the JPA sepcification makes use
	 * of exceptions outside its exception hierarchy, though they are all runtime exceptions.
	 * <p/>
	 * Any appropriate/needed calls to {@link #handlePersistenceException} are also made.
	 *
	 * @param e The Hibernate excepton.
	 *
	 * @return The JPA-specified exception
	 */
	public RuntimeException convert(HibernateException e);

	/**
	 * Delegates to {@link #convert} and then throws the given exception.
	 *
	 * @param e The exception being handled and finally thrown.
	 */
	public void throwPersistenceException(HibernateException e);

	public PersistenceException wrapStaleStateException(StaleStateException e);

	/**
	 * Convert from JPA 2 {@link LockModeType} & properties into {@link LockOptions}
	 *
	 * @param lockModeType is the requested lock type
	 * @param properties are the lock properties
	 *
	 * @return the LockOptions
	 */
	public LockOptions getLockRequest(LockModeType lockModeType, Map<String, Object> properties);

	public static interface Options {
		public static interface ResultMetadataValidator {
			public void validate(Type[] returnTypes);
		}

		/**
		 * Get the conversions for the individual tuples in the query results.
		 *
		 * @return Value conversions to be applied to the JPA QL results
		 */
		public List<ValueHandlerFactory.ValueHandler> getValueHandlers();

		/**
		 * Get the explicit parameter types.  Generally speaking these would apply to implicit named
		 * parameters.
		 *
		 * @return The
		 */
		public Map<String, Class> getNamedParameterExplicitTypes();

		public ResultMetadataValidator getResultMetadataValidator();
	}

	/**
	 * Used during "compiling" a JPA criteria query.
	 *
	 * @param jpaqlString The criteria query rendered as a JPA QL string
	 * @param resultClass The result type (the type expected in the result list)
	 * @param selection The selection(s)
	 * @param options The options to use to build the query.
	 * @param <T> The query type
	 *
	 * @return The typed query
	 */
	public <T> TypedQuery<T> createQuery(String jpaqlString, Class<T> resultClass, Selection selection, Options options);
}
