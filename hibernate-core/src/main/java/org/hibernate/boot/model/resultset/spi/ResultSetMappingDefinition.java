/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.resultset.spi;

import java.util.List;

import org.hibernate.Metamodel;
import org.hibernate.boot.model.resultset.internal.FetchDefinitionImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.ResultSetMappingDescriptor;
import org.hibernate.query.sql.spi.QueryResultBuilder;

/**
 * The boot-time model representation of native query SqlResultSetMapping
 *
 * @author Steve Ebersole
 */
public interface ResultSetMappingDefinition {
	String getName();

	List<Result> getResults();

	List<FetchDefinitionImpl> getFetches();

	ResultSetMappingDescriptor resolve(SessionFactoryImplementor sessionFactory);

	interface Result {
		QueryResultBuilder generateQueryResultBuilder(Metamodel metamodel);
	}

	interface Fetch {
		String getTableAlias();
		String getParentTableAlias();
		String getFetchedRoleName();
	}

	interface ScalarResult extends Result, ColumnMapping {
	}


	interface ColumnMapping {
		String getColumnAlias();
		String getTypeName();
	}

	interface InstantiationResult extends Result {
		String getTargetName();

		List<Argument> getArguments();

		interface Argument {
			Result getResult();

			String getAlias();
		}
	}

	interface PersistentCollectionResult extends Result {

	}

	interface EntityResult extends Result {
		String getEntityName();

		String getEntityClassName();

		String getTableAlias();
	}

	interface Attribute {
	}

}
