/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Closeable;
import java.io.Serializable;

import jakarta.persistence.criteria.CriteriaBuilder;

import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.QueryProducer;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;

/**
 * Declares operations that are common between {@link Session} and {@link StatelessSession}.
 *
 * @author Steve Ebersole
 */
public interface SharedSessionContract extends QueryProducer, Closeable, Serializable {
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
	 * Begin a unit of work and return the associated {@link Transaction} object.
	 * If a new underlying transaction is required, begin the transaction. Otherwise,
	 * continue the new work in the context of the existing underlying transaction.
	 *
	 * @return a {@link Transaction} instance
	 *
	 * @see #getTransaction()
	 */
	Transaction beginTransaction();

	/**
	 * Get the {@link Transaction} instance associated with this session.
	 *
	 * @return a Transaction instance
	 */
	Transaction getTransaction();

	/**
	 * Obtain a {@link ProcedureCall} based on a named template
	 *
	 * @param name The name given to the template
	 *
	 * @return The ProcedureCall
	 *
	 * @see jakarta.persistence.NamedStoredProcedureQuery
	 */
	ProcedureCall getNamedProcedureCall(String name);

	/**
	 * Create a {@link ProcedureCall} to a stored procedure.
	 *
	 * @param procedureName The name of the procedure.
	 *
	 * @return The representation of the procedure call.
	 */
	ProcedureCall createStoredProcedureCall(String procedureName);

	/**
	 * Create a {@link ProcedureCall} to a stored procedure with the given result
	 * set entity mappings. Each given class is considered a "root return".
	 *
	 * @param procedureName The name of the procedure.
	 * @param resultClasses The entity(s) to map the result on to.
	 *
	 * @return The representation of the procedure call.
	 */
	ProcedureCall createStoredProcedureCall(String procedureName, Class<?>... resultClasses);

	/**
	 * Create a {@link ProcedureCall} to a stored procedure with the given result
	 * set entity mappings.
	 *
	 * @param procedureName The name of the procedure.
	 * @param resultSetMappings The explicit result set mapping(s) to use for mapping the results
	 *
	 * @return The representation of the procedure call.
	 */
	ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings);

	/**
	 * Obtain a {@link ProcedureCall} based on a named template
	 *
	 * @param name The name given to the template
	 *
	 * @return The ProcedureCall
	 *
	 * @see jakarta.persistence.NamedStoredProcedureQuery
	 */
	ProcedureCall createNamedStoredProcedureQuery(String name);

	/**
	 * Create a {@link ProcedureCall} to a stored procedure.
	 *
	 * @param procedureName The name of the procedure.
	 *
	 * @return The representation of the procedure call.
	 */
	ProcedureCall createStoredProcedureQuery(String procedureName);

	/**
	 * Create a {@link ProcedureCall} to a stored procedure with the given result
	 * set entity mappings. Each given class is considered a "root return".
	 *
	 * @param procedureName The name of the procedure.
	 * @param resultClasses The entity(s) to map the result on to.
	 *
	 * @return The representation of the procedure call.
	 */
	ProcedureCall createStoredProcedureQuery(String procedureName, Class<?>... resultClasses);

	/**
	 * Create a {@link ProcedureCall} to a stored procedure with the given result set
	 * entity mappings.
	 *
	 * @param procedureName The name of the procedure.
	 * @param resultSetMappings The explicit result set mapping(s) to use for mapping the results
	 *
	 * @return The representation of the procedure call.
	 */
	ProcedureCall createStoredProcedureQuery(String procedureName, String... resultSetMappings);

	/**
	 * Get the session-level JDBC batch size for the current session.
	 *
	 * @return the current session-level JDBC batch size
	 *
	 * @since 5.2
	 *
	 * @see org.hibernate.boot.spi.SessionFactoryOptions#getJdbcBatchSize
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyJdbcBatchSize
	 */
	Integer getJdbcBatchSize();

	/**
	 * Set the session-level JDBC batch size. Overrides the
	 * {@link org.hibernate.boot.spi.SessionFactoryOptions#getJdbcBatchSize() factory-level}
	 * JDBC batch size defined by the configuration property
	 * {@link org.hibernate.cfg.AvailableSettings#STATEMENT_BATCH_SIZE}.
	 *
	 * @param jdbcBatchSize the new session-level JDBC batch size
	 *
	 * @since 5.2
	 *
	 * @see org.hibernate.boot.spi.SessionFactoryOptions#getJdbcBatchSize
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyJdbcBatchSize
	 */
	void setJdbcBatchSize(Integer jdbcBatchSize);

	/**
	 * Return an instance of {@link CriteriaBuilder}.
	 *
	 * @return an instance of CriteriaBuilder
	 *
	 * @throws IllegalStateException if the session has been closed
	 */
	HibernateCriteriaBuilder getCriteriaBuilder();

	/**
	 * Perform work using the {@link java.sql.Connection} underlying by this session.
	 *
	 * @param work The work to be performed.
	 *
	 * @throws HibernateException Generally indicates wrapped {@link java.sql.SQLException}
	 */
	default void doWork(Work work) throws HibernateException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Perform work using the {@link java.sql.Connection} underlying by this session,
	 * and return a result.
	 *
	 * @param work The work to be performed.
	 * @param <T> The type of the result returned from the work
	 *
	 * @return the result of calling {@link ReturningWork#execute}.
	 *
	 * @throws HibernateException Generally indicates wrapped {@link java.sql.SQLException}
	 */
	default <T> T doReturningWork(ReturningWork<T> work) throws HibernateException {
		throw new UnsupportedOperationException();
	}

}
