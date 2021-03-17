/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;

import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.QueryProducer;

/**
 * Contract methods shared between {@link Session} and {@link StatelessSession}.
 * <p/>
 * NOTE : Poorly named.  "shared" simply indicates that its a unified contract between {@link Session} and
 * {@link StatelessSession}.
 * 
 * @author Steve Ebersole
 */
public interface SharedSessionContract extends QueryProducer, Serializable {
	/**
	 * Obtain the tenant identifier associated with this session.
	 *
	 * @return The tenant identifier associated with this session, or {@code null}
	 */
	String getTenantIdentifier();

	/**
	 * End the session by releasing the JDBC connection and cleaning up.
	 *
	 * @throws HibernateException Indicates problems cleaning up.
	 */
	void close() throws HibernateException;

	/**
	 * Check if the session is still open.
	 *
	 * @return boolean
	 */
	boolean isOpen();

	/**
	 * Check if the session is currently connected.
	 *
	 * @return boolean
	 */
	boolean isConnected();

	/**
	 * Begin a unit of work and return the associated {@link Transaction} object.  If a new underlying transaction is
	 * required, begin the transaction.  Otherwise continue the new work in the context of the existing underlying
	 * transaction.
	 *
	 * @return a Transaction instance
	 *
	 * @see #getTransaction
	 */
	Transaction beginTransaction();

	/**
	 * Get the {@link Transaction} instance associated with this session.  The concrete type of the returned
	 * {@link Transaction} object is determined by the {@code hibernate.transaction_factory} property.
	 *
	 * @return a Transaction instance
	 */
	Transaction getTransaction();

	@Override
	org.hibernate.query.Query createQuery(String queryString);

	@Override
	org.hibernate.query.Query getNamedQuery(String queryName);

	/**
	 * Gets a ProcedureCall based on a named template
	 *
	 * @param name The name given to the template
	 *
	 * @return The ProcedureCall
	 *
	 * @see javax.persistence.NamedStoredProcedureQuery
	 */
	ProcedureCall getNamedProcedureCall(String name);

	/**
	 * Creates a call to a stored procedure.
	 *
	 * @param procedureName The name of the procedure.
	 *
	 * @return The representation of the procedure call.
	 */
	ProcedureCall createStoredProcedureCall(String procedureName);

	/**
	 * Creates a call to a stored procedure with specific result set entity mappings.  Each class named
	 * is considered a "root return".
	 *
	 * @param procedureName The name of the procedure.
	 * @param resultClasses The entity(s) to map the result on to.
	 *
	 * @return The representation of the procedure call.
	 */
	ProcedureCall createStoredProcedureCall(String procedureName, Class... resultClasses);

	/**
	 * Creates a call to a stored procedure with specific result set entity mappings.
	 *
	 * @param procedureName The name of the procedure.
	 * @param resultSetMappings The explicit result set mapping(s) to use for mapping the results
	 *
	 * @return The representation of the procedure call.
	 */
	ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings);

	/**
	 * Create {@link Criteria} instance for the given class (entity or subclasses/implementors).
	 *
	 * @param persistentClass The class, which is an entity, or has entity subclasses/implementors
	 *
	 * @return The criteria instance for manipulation and execution
	 *
	 * @deprecated (since 5.2) for Session, use the JPA Criteria
	 */
	@Deprecated
	Criteria createCriteria(Class persistentClass);

	/**
	 * Create {@link Criteria} instance for the given class (entity or subclasses/implementors), using a specific
	 * alias.
	 *
	 * @param persistentClass The class, which is an entity, or has entity subclasses/implementors
	 * @param alias The alias to use
	 *
	 * @return The criteria instance for manipulation and execution
	 *
	 * @deprecated (since 5.2) for Session, use the JPA Criteria
	 */
	@Deprecated
	Criteria createCriteria(Class persistentClass, String alias);

	/**
	 * Create {@link Criteria} instance for the given entity name.
	 *
	 * @param entityName The entity name

	 * @return The criteria instance for manipulation and execution
	 *
	 * @deprecated (since 5.2) for Session, use the JPA Criteria
	 */
	@Deprecated
	Criteria createCriteria(String entityName);

	/**
	 * Create {@link Criteria} instance for the given entity name, using a specific alias.
	 *
	 * @param entityName The entity name
	 * @param alias The alias to use
	 *
	 * @return The criteria instance for manipulation and execution
	 *
	 * @deprecated (since 5.2) for Session, use the JPA Criteria
	 */
	@Deprecated
	Criteria createCriteria(String entityName, String alias);

	/**
	 * Get the Session-level JDBC batch size for the current Session.
	 * Overrides the SessionFactory JDBC batch size defined by the {@code hibernate.default_batch_fetch_size} configuration property for the scope of the current {@code Session}.
	 *
	 * @return Session-level JDBC batch size
	 *
	 * @since 5.2
	 *
	 * @see org.hibernate.boot.spi.SessionFactoryOptions#getJdbcBatchSize
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyJdbcBatchSize
	 */
	Integer getJdbcBatchSize();

	/**
	 * Set the Session-level JDBC batch size.
	 * Overrides the SessionFactory JDBC batch size defined by the {@code hibernate.default_batch_fetch_size} configuration property for the scope of the current {@code Session}.
	 *
	 * @param jdbcBatchSize Session-level JDBC batch size
	 *
	 * @since 5.2
	 *
	 * @see org.hibernate.boot.spi.SessionFactoryOptions#getJdbcBatchSize
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyJdbcBatchSize
	 */
	void setJdbcBatchSize(Integer jdbcBatchSize);

	/**
	 * Controller for allowing users to perform JDBC related work using the Connection managed by this Session.
	 *
	 * @param work The work to be performed.
	 * @throws HibernateException Generally indicates wrapped {@link java.sql.SQLException}
	 */
	default void doWork(Work work) throws HibernateException {
		throw new UnsupportedOperationException( "The doWork method has not been implemented in this implementation of org.hibernate.engine.spi.SharedSessionContractImplemento" );
	}

	/**
	 * Controller for allowing users to perform JDBC related work using the Connection managed by this Session.  After
	 * execution returns the result of the {@link ReturningWork#execute} call.
	 *
	 * @param work The work to be performed.
	 * @param <T> The type of the result returned from the work
	 *
	 * @return the result from calling {@link ReturningWork#execute}.
	 *
	 * @throws HibernateException Generally indicates wrapped {@link java.sql.SQLException}
	 */
	default <T> T doReturningWork(ReturningWork<T> work) throws HibernateException {
		throw new UnsupportedOperationException( "The doReturningWork method has not been implemented in this implementation of org.hibernate.engine.spi.SharedSessionContractImplemento" );
	}

}
