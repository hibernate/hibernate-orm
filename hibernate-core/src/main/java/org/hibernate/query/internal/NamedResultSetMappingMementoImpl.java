/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import org.hibernate.SessionFactory;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.named.ResultMemento;
import org.hibernate.query.results.spi.ResultSetMapping;

import jakarta.persistence.sql.MappingElement;

import java.util.List;
import java.util.function.Consumer;

import static jakarta.persistence.sql.ResultSetMapping.compound;
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
		if ( getResultMementos().size() == 1 ) {
			var resultMemento = getResultMementos().get( 0 );
			return resultMemento.canBeTreatedAsResultSetMapping( resultType, sessionFactory );
		}
		return resultType.isAssignableFrom( Object[].class );
	}

	@Override
	public <R> jakarta.persistence.sql.ResultSetMapping<R> toJpaMapping(SessionFactory sessionFactory) {
		if ( getResultMementos().size() == 1 ) {
			var resultMemento = getResultMementos().get( 0 );
			return resultMemento.toJpaMapping( sessionFactory );
		}

		final var elements = new MappingElement<?>[getResultMementos().size()];
		for ( int i = 0; i < elements.length; i++ ) {
			final var resultSetMapping = getResultMementos().get( i ).toJpaMapping( sessionFactory );
			if ( resultSetMapping instanceof MappingElement<?> element ) {
				elements[i] = element;
			}
			else {
				throw new UnsupportedOperationException(
						"Result set mapping element does not implement MappingElement: " + resultSetMapping
				);
			}
		}

		//noinspection unchecked
		return (jakarta.persistence.sql.ResultSetMapping<R>) compound( elements );
	}
}
