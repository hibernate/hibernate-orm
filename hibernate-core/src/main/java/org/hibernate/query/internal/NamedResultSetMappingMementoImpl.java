/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.query.results.ScalarResultBuilder;

/**
 * Standard `NamedResultSetMappingMemento` implementation
 *
 * @author Steve Ebersole
 */
public class NamedResultSetMappingMementoImpl implements NamedResultSetMappingMemento {
	private final String name;

	private final List<ScalarResultBuilder> scalarResultBuilders;

	public NamedResultSetMappingMementoImpl(
			String name,
			List<ScalarResultBuilder> scalarResultBuilders,
			SessionFactoryImplementor factory) {
		this.name = name;
		this.scalarResultBuilders = scalarResultBuilders;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void resolve(
			ResultSetMapping resultSetMapping,
			Consumer<String> querySpaceConsumer,
			SessionFactoryImplementor sessionFactory) {
		scalarResultBuilders.forEach(
				builder -> resultSetMapping.addResultBuilder(
						(jdbcResultsMetadata, legacyFetchResolver, sqlSelectionConsumer, sessionFactory1) ->
								builder.buildReturn(
										jdbcResultsMetadata,
										legacyFetchResolver,
										sqlSelectionConsumer,
										sessionFactory
								)
				)
		);
	}
}
