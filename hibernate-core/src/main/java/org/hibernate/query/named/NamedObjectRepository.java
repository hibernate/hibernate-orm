/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.named;

import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.query.hql.spi.NamedHqlQueryMemento;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sql.spi.NamedNativeQueryMemento;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;

/**
 * Repository for references to named things related with queries.  This includes
 * named HQL, JPAQL, native and procedure queries as well as named result-set
 * mappings
 *
 * @author Steve Ebersole
 */
@Incubating
public interface NamedObjectRepository {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named SQM Memento

	NamedSqmQueryMemento getSqmQueryMemento(String queryName);
	void visitSqmQueryMementos(Consumer<NamedSqmQueryMemento> action);
	void registerSqmQueryMemento(String name, NamedSqmQueryMemento descriptor);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named HQL Memento

	NamedHqlQueryMemento getHqlQueryMemento(String queryName);
	void visitHqlQueryMementos(Consumer<NamedHqlQueryMemento> action);
	void registerHqlQueryMemento(String name, NamedHqlQueryMemento descriptor);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named NativeQuery Memento

	NamedNativeQueryMemento getNativeQueryMemento(String queryName);
	void visitNativeQueryMementos(Consumer<NamedNativeQueryMemento> action);
	void registerNativeQueryMemento(String name, NamedNativeQueryMemento descriptor);


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
	 * Resolve the named query with the given name.
	 */
	NamedQueryMemento resolve(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor bootMetamodel,
			String registrationName);

	/**
	 * Prepare for runtime use
	 */
	void prepare(SessionFactoryImplementor sessionFactory, MetadataImplementor bootMetamodel, BootstrapContext bootstrapContext);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Shutdown

	/**
	 * Release any held resources
	 */
	void close();

}
