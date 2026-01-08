/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.SessionFactory;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.named.ResultMemento;
import org.hibernate.query.results.ResultSetMapping;

import static java.util.Collections.unmodifiableList;

/**
 * Standard {@link NamedResultSetMappingMemento} implementation
 *
 * @author Steve Ebersole
 */
public class NamedResultSetMappingMementoImpl implements NamedResultSetMappingMemento {
	private final String name;
	private final List<ResultMemento> resultMementos;

	public NamedResultSetMappingMementoImpl(
			String name,
			List<ResultMemento> resultMementos) {
		this.name = name;
		this.resultMementos = resultMementos;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<ResultMemento> getResultMementos() {
		return unmodifiableList( resultMementos );
	}

	@Override
	public void resolve(
			ResultSetMapping resultSetMapping,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		resultMementos.forEach(
				memento -> resultSetMapping.addResultBuilder( memento.resolve( querySpaceConsumer, context ) )
		);
	}

	@Override
	public <R> boolean canBeTreatedAsResultSetMapping(Class<R> resultType, SessionFactory sessionFactory) {
		if ( getResultMementos().size() != 1 ) {
			return false;
		}
		var resultMemento = getResultMementos().get( 0 );
		return resultMemento.canBeTreatedAsResultSetMapping( resultType, sessionFactory );
	}

	@Override
	public <R> jakarta.persistence.sql.ResultSetMapping<R> toJpaMapping(SessionFactory sessionFactory) {
		assert getResultMementos().size() == 1;
		var resultMemento = getResultMementos().get( 0 );

		return resultMemento.toJpaMapping( sessionFactory );
	}
}
