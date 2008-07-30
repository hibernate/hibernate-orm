/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.classic;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.type.Type;

/**
 * An extension of the <tt>Session</tt> API, including all
 * deprecated methods from Hibernate2. This interface is
 * provided to allow easier migration of existing applications.
 * New code should use <tt>org.hibernate.Session</tt>.
 * @author Gavin King
 */
public interface Session extends org.hibernate.Session {

	/**
	 * Copy the state of the given object onto the persistent object with the same
	 * identifier. If there is no persistent instance currently associated with
	 * the session, it will be loaded. Return the persistent instance. If the
	 * given instance is unsaved or does not exist in the database, save it and
	 * return it as a newly persistent instance. Otherwise, the given instance
	 * does not become associated with the session.
	 *
	 * @deprecated use {@link org.hibernate.Session#merge(Object)}
	 *
	 * @param object a transient instance with state to be copied
	 * @return an updated persistent instance
	 */
	public Object saveOrUpdateCopy(Object object) throws HibernateException;

	/**
	 * Copy the state of the given object onto the persistent object with the
	 * given identifier. If there is no persistent instance currently associated
	 * with the session, it will be loaded. Return the persistent instance. If
	 * there is no database row with the given identifier, save the given instance
	 * and return it as a newly persistent instance. Otherwise, the given instance
	 * does not become associated with the session.
	 *
	 * @deprecated with no replacement
	 *
	 * @param object a persistent or transient instance with state to be copied
	 * @param id the identifier of the instance to copy to
	 * @return an updated persistent instance
	 */
	public Object saveOrUpdateCopy(Object object, Serializable id) throws HibernateException;

	/**
	 * Copy the state of the given object onto the persistent object with the same
	 * identifier. If there is no persistent instance currently associated with
	 * the session, it will be loaded. Return the persistent instance. If the
	 * given instance is unsaved or does not exist in the database, save it and
	 * return it as a newly persistent instance. Otherwise, the given instance
	 * does not become associated with the session.
	 *
	 * @deprecated use {@link org.hibernate.Session#merge(String, Object)}
	 *
	 * @param object a transient instance with state to be copied
	 * @return an updated persistent instance
	 */
	public Object saveOrUpdateCopy(String entityName, Object object) throws HibernateException;

	/**
	 * Copy the state of the given object onto the persistent object with the
	 * given identifier. If there is no persistent instance currently associated
	 * with the session, it will be loaded. Return the persistent instance. If
	 * there is no database row with the given identifier, save the given instance
	 * and return it as a newly persistent instance. Otherwise, the given instance
	 * does not become associated with the session.
	 *
	 * @deprecated with no replacement
	 *
	 * @param object a persistent or transient instance with state to be copied
	 * @param id the identifier of the instance to copy to
	 * @return an updated persistent instance
	 */
	public Object saveOrUpdateCopy(String entityName, Object object, Serializable id) throws HibernateException;

	/**
	 * Execute a query.
	 *
	 * @deprecated use {@link #createQuery}.{@link Query#list()}
	 *
	 * @param query a query expressed in Hibernate's query language
	 * @return a distinct list of instances (or arrays of instances)
	 * @throws HibernateException
	 */
	public List find(String query) throws HibernateException;
			
	/**
	 * Execute a query with bind parameters, binding a value to a "?" parameter
	 * in the query string.
	 *
	 * @deprecated use {@link #createQuery}.setXYZ.{@link Query#list()}
	 *
	 * @param query the query string
	 * @param value a value to be bound to a "?" placeholder (JDBC IN parameter).
	 * @param type the Hibernate type of the value
	 * @see org.hibernate.Hibernate for access to <tt>Type</tt> instances
	 * @return a distinct list of instances (or arrays of instances)
	 * @throws HibernateException
	 */
	public List find(String query, Object value, Type type) throws HibernateException;
	
	/**
	 * Execute a query with bind parameters, binding an array of values to "?"
	 * parameters in the query string.
	 *
	 * @deprecated use {@link #createQuery}.setXYZ.{@link Query#list()}
	 *
	 * @param query the query string
	 * @param values an array of values to be bound to the "?" placeholders (JDBC IN parameters).
	 * @param types an array of Hibernate types of the values
	 * @see org.hibernate.Hibernate for access to <tt>Type</tt> instances
	 * @return a distinct list of instances
	 * @throws HibernateException
	 */
	public List find(String query, Object[] values, Type[] types) throws HibernateException;
	
	/**
	 * Execute a query and return the results in an iterator. If the query has multiple
	 * return values, values will be returned in an array of type <tt>Object[].</tt><br>
	 * <br>
	 * Entities returned as results are initialized on demand. The first SQL query returns
	 * identifiers only. So <tt>iterate()</tt> is usually a less efficient way to retrieve
	 * objects than <tt>find()</tt>.
	 * 
	 * @deprecated use {@link #createQuery}.{@link Query#iterate}
	 *
	 * @param query the query string
	 * @return an iterator
	 * @throws HibernateException
	 */
	public Iterator iterate(String query) throws HibernateException;
	
	/**
	 * Execute a query and return the results in an iterator. Write the given value to "?"
	 * in the query string. If the query has multiple return values, values will be returned
	 * in an array of type <tt>Object[]</tt>.<br>
	 * <br>
	 * Entities returned as results are initialized on demand. The first SQL query returns
	 * identifiers only. So <tt>iterate()</tt> is usually a less efficient way to retrieve
	 * objects than <tt>find()</tt>.
	 *
	 * @deprecated use {@link #createQuery}.setXYZ.{@link Query#iterate}
	 *
	 * @param query the query string
	 * @param value a value to be witten to a "?" placeholder in the query string
	 * @param type the hibernate type of value
	 * @return an iterator
	 * @throws HibernateException
	 */
	public Iterator iterate(String query, Object value, Type type) throws HibernateException;
	
	/**
	 * Execute a query and return the results in an iterator. Write the given values to "?"
	 * in the query string. If the query has multiple return values, values will be returned
	 * in an array of type <tt>Object[]</tt>.<br>
	 * <br>
	 * Entities returned as results are initialized on demand. The first SQL query returns
	 * identifiers only. So <tt>iterate()</tt> is usually a less efficient way to retrieve
	 * objects than <tt>find()</tt>.
	 *
	 * @deprecated use {@link #createQuery}.setXYZ.{@link Query#iterate}
	 *
	 * @param query the query string
	 * @param values a list of values to be written to "?" placeholders in the query
	 * @param types a list of Hibernate types of the values
	 * @return an iterator
	 * @throws HibernateException
	 */
	public Iterator iterate(String query, Object[] values, Type[] types) throws HibernateException;
	
	/**
	 * Apply a filter to a persistent collection. A filter is a Hibernate query that may refer to
	 * <tt>this</tt>, the collection element. Filters allow efficient access to very large lazy
	 * collections. (Executing the filter does not initialize the collection.)
	 * 
	 * @deprecated use {@link #createFilter(Object, String)}.{@link Query#list}
	 *
	 * @param collection a persistent collection to filter
	 * @param filter a filter query string
	 * @return Collection the resulting collection
	 * @throws HibernateException
	 */
	public Collection filter(Object collection, String filter) throws HibernateException;
	
	/**
	 * Apply a filter to a persistent collection. A filter is a Hibernate query that may refer to
	 * <tt>this</tt>, the collection element.
	 *
	 * @deprecated use {@link #createFilter(Object, String)}.setXYZ.{@link Query#list}
	 *
	 * @param collection a persistent collection to filter
	 * @param filter a filter query string
	 * @param value a value to be witten to a "?" placeholder in the query string
	 * @param type the hibernate type of value
	 * @return Collection
	 * @throws HibernateException
	 */
	public Collection filter(Object collection, String filter, Object value, Type type) throws HibernateException;
	
	/**
	 * Apply a filter to a persistent collection.
	 *
	 * Bind the given parameters to "?" placeholders. A filter is a Hibernate query that
	 * may refer to <tt>this</tt>, the collection element.
	 *
	 * @deprecated use {@link #createFilter(Object, String)}.setXYZ.{@link Query#list}
	 *
	 * @param collection a persistent collection to filter
	 * @param filter a filter query string
	 * @param values a list of values to be written to "?" placeholders in the query
	 * @param types a list of Hibernate types of the values
	 * @return Collection
	 * @throws HibernateException
	 */
	public Collection filter(Object collection, String filter, Object[] values, Type[] types) throws HibernateException;
	
	/**
	 * Delete all objects returned by the query. Return the number of objects deleted.
	 * <p/>
	 * Note that this is very different from the delete-statement support added in HQL
	 * since 3.1.  The functionality here is to actually peform the query and then iterate
	 * the results calling {@link #delete(Object)} individually.
	 * 
	 * @deprecated consider using HQL delete statements
	 *
	 * @param query the query string
	 * @return the number of instances deleted
	 * @throws HibernateException
	 */
	public int delete(String query) throws HibernateException;
	
	/**
	 * Delete all objects returned by the query. Return the number of objects deleted.
	 * <p/>
	 * Note that this is very different from the delete-statement support added in HQL
	 * since 3.1.  The functionality here is to actually peform the query and then iterate
	 * the results calling {@link #delete(Object)} individually.
	 *
	 * @deprecated consider using HQL delete statements
	 *
	 * @param query the query string
	 * @param value a value to be witten to a "?" placeholder in the query string.
	 * @param type the hibernate type of value.
	 * @return the number of instances deleted
	 * @throws HibernateException
	 */
	public int delete(String query, Object value, Type type) throws HibernateException;
	
	/**
	 * Delete all objects returned by the query. Return the number of objects deleted.
	 * <p/>
	 * Note that this is very different from the delete-statement support added in HQL
	 * since 3.1.  The functionality here is to actually peform the query and then iterate
	 * the results calling {@link #delete(Object)} individually.
	 *
	 * @deprecated consider using HQL delete statements
	 *
	 * @param query the query string
	 * @param values a list of values to be written to "?" placeholders in the query.
	 * @param types a list of Hibernate types of the values
	 * @return the number of instances deleted
	 * @throws HibernateException
	 */
	public int delete(String query, Object[] values, Type[] types) throws HibernateException;


	/**
	 * Create a new instance of <tt>Query</tt> for the given SQL string.
	 *
	 * @deprecated will be replaced with a more Query like interface in later release
	 *
	 * @param sql a query expressed in SQL
	 * @param returnAlias a table alias that appears inside <tt>{}</tt> in the SQL string
	 * @param returnClass the returned persistent class
	 */
	public Query createSQLQuery(String sql, String returnAlias, Class returnClass);
	
	/**
	 * Create a new instance of <tt>Query</tt> for the given SQL string.
	 *
	 * @deprecated will be replaced with a more Query like interface in later release
	 *
	 * @param sql a query expressed in SQL
	 * @param returnAliases an array of table aliases that appear inside <tt>{}</tt> in the SQL string
	 * @param returnClasses the returned persistent classes
	 */
	public Query createSQLQuery(String sql, String[] returnAliases, Class[] returnClasses);
	
	
	/**
	 * Persist the given transient instance, using the given identifier.  This operation 
	 * cascades to associated instances if the association is mapped with 
	 * <tt>cascade="save-update"</tt>.
	 *
	 * @deprecated declare identifier properties for all classes
	 *
	 * @param object a transient instance of a persistent class
	 * @param id an unused valid identifier
	 * @throws HibernateException
	 */
	public void save(Object object, Serializable id) throws HibernateException;

	/**
	 * Persist the given transient instance, using the given identifier. This operation 
	 * cascades to associated instances if the association is mapped with 
	 * <tt>cascade="save-update"</tt>.
	 *
	 * @deprecated declare identifier properties for all classes
	 *
	 * @param object a transient instance of a persistent class
	 * @param id an unused valid identifier
	 * @throws HibernateException
	 */
	public void save(String entityName, Object object, Serializable id) throws HibernateException;

	/**
	 * Update the persistent state associated with the given identifier. An exception
	 * is thrown if there is a persistent instance with the same identifier in the
	 * current session. This operation cascades to associated instances 
	 * if the association is mapped with <tt>cascade="save-update"</tt>.
	 *
	 * @deprecated declare identifier properties for all classes
	 *
	 * @param object a detached instance containing updated state
	 * @param id identifier of persistent instance
	 * @throws HibernateException
	 */
	public void update(Object object, Serializable id) throws HibernateException;

	/**
	 * Update the persistent state associated with the given identifier. An exception
	 * is thrown if there is a persistent instance with the same identifier in the
	 * current session. This operation cascades to associated instances 
	 * if the association is mapped with <tt>cascade="save-update"</tt>.
	 * 
	 * @deprecated declare identifier properties for all classes
	 *
	 * @param object a detached instance containing updated state
	 * @param id identifier of persistent instance
	 * @throws HibernateException
	 */
	public void update(String entityName, Object object, Serializable id) throws HibernateException;
	
}
