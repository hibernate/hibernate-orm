/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.query.hql.spi.NamedHqlQueryMemento;
import org.hibernate.query.sql.spi.NamedNativeQueryMemento;

/**
 * @author Steve Ebersole
 */
@Incubating
public interface NamedQueryRepository {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NamedHqlQueryMemento

	NamedHqlQueryMemento getHqlQueryMemento(String queryName);
	void visitHqlQueryMementos(Consumer<NamedHqlQueryMemento> action);
	void registerHqlQueryMemento(String name, NamedHqlQueryMemento descriptor);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NamedNativeQueryMemento

	NamedNativeQueryMemento getNativeQueryMemento(String queryName);
	void visitNativeQueryMementos(Consumer<NamedNativeQueryMemento> action);
	void registerNativeQueryMemento(String name, NamedNativeQueryMemento descriptor);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NamedCallableQueryMemento

	NamedCallableQueryMemento getCallableQueryMemento(String name);
	void visitCallableQueryMementos(Consumer<NamedCallableQueryMemento> action);
	void registerCallableQueryMemento(String name, NamedCallableQueryMemento memento);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ResultSetMappingDescriptor

	NamedResultSetMappingMemento getResultSetMappingMemento(String mappingName);
	void visitResultSetMappingMementos(Consumer<NamedResultSetMappingMemento> action);
	void registerResultSetMappingMemento(String name, NamedResultSetMappingMemento memento);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// boot-time

	Map<String, HibernateException> checkNamedQueries(QueryEngine queryPlanCache);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// shut down

	void close();


}
