/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.named;

import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sql.spi.NamedNativeQueryMemento;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;

import jakarta.persistence.TypedQueryReference;

/**
 * Repository for references to named things related to queries. This includes:
 * <ul>
 * <li>named HQL, JPQL, native, and procedure queries,
 * <li>along with named result set mappings.
 * </ul>
 *
 * @author Steve Ebersole
 */
@Incubating
public interface NamedObjectRepository {

	<R> Map<String, TypedQueryReference<R>> getNamedQueries(Class<R> resultType);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named SQM Memento

	NamedSqmQueryMemento<?> getSqmQueryMemento(String queryName);
	void visitSqmQueryMementos(Consumer<NamedSqmQueryMemento<?>> action);
	void registerSqmQueryMemento(String name, NamedSqmQueryMemento<?> descriptor);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named NativeQuery Memento

	NamedNativeQueryMemento<?> getNativeQueryMemento(String queryName);
	void visitNativeQueryMementos(Consumer<NamedNativeQueryMemento<?>> action);
	void registerNativeQueryMemento(String name, NamedNativeQueryMemento<?> descriptor);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named CallableQuery Memento

	NamedCallableQueryMemento getCallableQueryMemento(String name);
	void visitCallableQueryMementos(Consumer<NamedCallableQueryMemento> action);
	void registerCallableQueryMemento(String name, NamedCallableQueryMemento memento);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named ResultSetMapping memento

	NamedResultSetMappingMemento getResultSetMappingMemento(String mappingName);
	void visitResultSetMappingMementos(Consumer<NamedResultSetMappingMemento> action);
	void registerResultSetMappingMemento(String name, NamedResultSetMappingMemento memento);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// boot-time

	/**
	 * Perform a validity check on all named queries
	 */
	Map<String, HibernateException> checkNamedQueries(QueryEngine queryPlanCache);

	/**
	 * Validate the named queries and throw an exception if any are broken
	 */
	void validateNamedQueries(QueryEngine queryEngine);

	/**
	 * Resolve the named query with the given name.
	 */
	NamedQueryMemento resolve(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor bootMetamodel,
			String registrationName);

	/**
	 * Prepare for runtime use
	 */
	// TODO: avoid passing in the whole SessionFactory here, it's not necessary
	void prepare(SessionFactoryImplementor sessionFactory, Metadata bootMetamodel);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Shutdown

	/**
	 * Release any held resources
	 */
	void close();

}
