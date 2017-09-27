/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.resultset.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.resultset.spi.ResultSetMappingDefinition;
import org.hibernate.query.sql.spi.QueryResultBuilder;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class PersistentCollectionResultDefinitionImpl implements ResultSetMappingDefinition.PersistentCollectionResult {
	@Override
	public QueryResultBuilder generateQueryResultBuilder(TypeConfiguration typeConfiguration) {
		throw new NotYetImplementedFor6Exception();
	}
}
