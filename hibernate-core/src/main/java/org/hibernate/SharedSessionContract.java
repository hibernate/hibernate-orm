/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;

import org.hibernate.procedure.ProcedureCall;

/**
 * Contract methods shared between {@link Session} and {@link StatelessSession}.
 * 
 * @author Steve Ebersole
 */
public interface SharedSessionContract extends Serializable {
	/**
	 * Obtain the tenant identifier associated with this session.
	 *
	 * @return The tenant identifier associated with this session, or {@code null}
	 */
	public String getTenantIdentifier();

	/**
	 * Begin a unit of work and return the associated {@link Transaction} object.  If a new underlying transaction is
	 * required, begin the transaction.  Otherwise continue the new work in the context of the existing underlying
	 * transaction.
	 *
	 * @return a Transaction instance
	 *
	 * @see #getTransaction
	 */
	public Transaction beginTransaction();

	/**
	 * Get the {@link Transaction} instance associated with this session.  The concrete type of the returned
	 * {@link Transaction} object is determined by the {@code hibernate.transaction_factory} property.
	 *
	 * @return a Transaction instance
	 */
	public Transaction getTransaction();

	/**
	 * Create a {@link Query} instance for the named query string defined in the metadata.
	 *
	 * @param queryName the name of a query defined externally
	 *
	 * @return The query instance for manipulation and execution
	 */
	public Query getNamedQuery(String queryName);

	/**
	 * Create a {@link Query} instance for the given HQL query string.
	 *
	 * @param queryString The HQL query
	 *
	 * @return The query instance for manipulation and execution
	 */
	public Query createQuery(String queryString);

	/**
	 * Create a {@link SQLQuery} instance for the given SQL query string.
	 *
	 * @param queryString The SQL query
	 * 
	 * @return The query instance for manipulation and execution
	 */
	public SQLQuery createSQLQuery(String queryString);

	/**
	 * Gets a ProcedureCall based on a named template
	 *
	 * @param name The name given to the template
	 *
	 * @return The ProcedureCall
	 *
	 * @see javax.persistence.NamedStoredProcedureQuery
	 */
	public ProcedureCall getNamedProcedureCall(String name);

	/**
	 * Creates a call to a stored procedure.
	 *
	 * @param procedureName The name of the procedure.
	 *
	 * @return The representation of the procedure call.
	 */
	public ProcedureCall createStoredProcedureCall(String procedureName);

	/**
	 * Creates a call to a stored procedure with specific result set entity mappings.  Each class named
	 * is considered a "root return".
	 *
	 * @param procedureName The name of the procedure.
	 * @param resultClasses The entity(s) to map the result on to.
	 *
	 * @return The representation of the procedure call.
	 */
	public ProcedureCall createStoredProcedureCall(String procedureName, Class... resultClasses);

	/**
	 * Creates a call to a stored procedure with specific result set entity mappings.
	 *
	 * @param procedureName The name of the procedure.
	 * @param resultSetMappings The explicit result set mapping(s) to use for mapping the results
	 *
	 * @return The representation of the procedure call.
	 */
	public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings);

	/**
	 * Create {@link Criteria} instance for the given class (entity or subclasses/implementors).
	 *
	 * @param persistentClass The class, which is an entity, or has entity subclasses/implementors
	 *
	 * @return The criteria instance for manipulation and execution
	 */
	public Criteria createCriteria(Class persistentClass);

	/**
	 * Create {@link Criteria} instance for the given class (entity or subclasses/implementors), using a specific
	 * alias.
	 *
	 * @param persistentClass The class, which is an entity, or has entity subclasses/implementors
	 * @param alias The alias to use
	 *
	 * @return The criteria instance for manipulation and execution
	 */
	public Criteria createCriteria(Class persistentClass, String alias);

	/**
	 * Create {@link Criteria} instance for the given entity name.
	 *
	 * @param entityName The entity name

	 * @return The criteria instance for manipulation and execution
	 */
	public Criteria createCriteria(String entityName);

	/**
	 * Create {@link Criteria} instance for the given entity name, using a specific alias.
	 *
	 * @param entityName The entity name
	 * @param alias The alias to use
	 *
	 * @return The criteria instance for manipulation and execution
	 */
	public Criteria createCriteria(String entityName, String alias);
}
