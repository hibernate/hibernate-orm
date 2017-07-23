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
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.procedure.ProcedureCallMemento;

/**
 * @author Steve Ebersole
 */
@Incubating
public interface NamedQueryRepository {
	NamedQueryDefinition getNamedQueryDefinition(String queryName);

	NamedSQLQueryDefinition getNamedSQLQueryDefinition(String queryName);

	ProcedureCallMemento getNamedProcedureCallMemento(String name);

	ResultSetMappingDefinition getResultSetMappingDefinition(String mappingName);

	void registerNamedQueryDefinition(String name, NamedQueryDefinition definition);

	void registerNamedSQLQueryDefinition(String name, NamedSQLQueryDefinition definition);

	void registerNamedProcedureCallMemento(String name, ProcedureCallMemento memento);

	Map<String,HibernateException> checkNamedQueries(QueryEngine queryPlanCache);
}
