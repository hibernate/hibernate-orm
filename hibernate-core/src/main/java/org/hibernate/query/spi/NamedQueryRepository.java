/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.query.named.spi.NamedHqlQueryDescriptor;
import org.hibernate.query.named.spi.NamedNativeQueryDescriptor;

/**
 * Repository for named query-related objects
 *
 * @author Steve Ebersole
 */
@Incubating
public interface NamedQueryRepository {
	NamedHqlQueryDescriptor getNamedHqlDescriptor(String queryName);

	NamedNativeQueryDescriptor getNamedNativeDescriptor(String queryName);

	ProcedureCallMemento getNamedProcedureCallMemento(String name);

	ResultSetMappingDescriptor getResultSetMappingDescriptor(String mappingName);

	void registerNamedHqlQueryDescriptor(String name, NamedHqlQueryDescriptor descriptor);

	void registerNamedNativeQueryDescriptor(String name, NamedNativeQueryDescriptor descriptor);

	void registerNamedProcedureCallMemento(String name, ProcedureCallMemento memento);

	Map<String,HibernateException> checkNamedQueries(QueryEngine queryPlanCache);
}
