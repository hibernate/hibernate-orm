/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.resultset.internal;

import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.Metamodel;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.resultset.spi.ResultSetMappingDefinition;
import org.hibernate.query.sql.spi.QueryResultBuilder;

/**
 * @author Steve Ebersole
 */
public class PersistentCollectionResultDefinitionImpl implements ResultSetMappingDefinition.PersistentCollectionResult {
	public PersistentCollectionResultDefinitionImpl(
			String className,
			String substring,
			String alias,
			LockMode lockMode,
			Map<String, String[]> extractPropertyResults) {

	}

	@Override
	public QueryResultBuilder generateQueryResultBuilder(Metamodel metamodel) {
		throw new NotYetImplementedFor6Exception();
	}
}
