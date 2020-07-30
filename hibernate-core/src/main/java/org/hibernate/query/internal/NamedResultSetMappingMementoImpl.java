/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.results.EntityResultBuilder;
import org.hibernate.query.results.InstantiationResultBuilder;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.query.results.ScalarResultBuilder;

/**
 * Standard `NamedResultSetMappingMemento` implementation
 *
 * @author Steve Ebersole
 */
public class NamedResultSetMappingMementoImpl implements NamedResultSetMappingMemento {
	private final String name;

	private final List<ResultBuilder> resultBuilders;

	public NamedResultSetMappingMementoImpl(
			String name,
			List<EntityResultBuilder> entityResultBuilders,
			List<InstantiationResultBuilder> instantiationResultBuilders,
			List<ScalarResultBuilder> scalarResultBuilders,
			SessionFactoryImplementor factory) {
		this.name = name;

		final int totalNumberOfBuilders = entityResultBuilders.size()
				+ instantiationResultBuilders.size()
				+ scalarResultBuilders.size();
		this.resultBuilders = new ArrayList<>( totalNumberOfBuilders );

		resultBuilders.addAll( entityResultBuilders );
		resultBuilders.addAll( instantiationResultBuilders );
		resultBuilders.addAll( scalarResultBuilders );
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
		resultBuilders.forEach( resultSetMapping::addResultBuilder );
	}
}
