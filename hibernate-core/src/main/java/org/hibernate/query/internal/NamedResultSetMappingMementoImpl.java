/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.sql.ColumnMapping;
import jakarta.persistence.sql.CompoundMapping;
import jakarta.persistence.sql.ConstructorMapping;
import jakarta.persistence.sql.EntityMapping;
import jakarta.persistence.sql.MappingElement;
import jakarta.persistence.sql.TupleMapping;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
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

	public static <T> NamedResultSetMappingMementoImpl from(
			jakarta.persistence.sql.ResultSetMapping<T> resultSetMapping,
			SessionFactoryImplementor factory) {
		final List<ResultMemento> results;
		if ( resultSetMapping instanceof ColumnMapping<T> columnMapping ) {
			results = List.of( ResultMementoBasicStandard.from( columnMapping, factory ) );
		}
		else if ( resultSetMapping instanceof ConstructorMapping<T> constructorMapping ) {
			results = List.of( ResultMementoInstantiationStandard.from( constructorMapping, factory ) );
		}
		else if ( resultSetMapping instanceof EntityMapping<T> entityMapping ) {
			results = List.of( ResultMementoEntityJpa.from( entityMapping, factory ) );
		}
		else if ( resultSetMapping instanceof TupleMapping tupleMapping ) {
			results = List.of( ResultMementoTuple.from( tupleMapping, factory ) );
		}
		else if ( resultSetMapping instanceof CompoundMapping compoundMapping ) {
			results = CollectionHelper.arrayList( compoundMapping.elements().length );
			for ( int i = 0; i < compoundMapping.elements().length; i++ ) {
				final MappingElement<?> mappingElement = compoundMapping.elements()[i];
				if ( mappingElement instanceof ColumnMapping<?> columnMapping ) {
					results.add( ResultMementoBasicStandard.from( columnMapping, factory ) );
				}
				else if ( mappingElement instanceof ConstructorMapping<?> constructorMapping ) {
					results.add( ResultMementoInstantiationStandard.from( constructorMapping, factory ) );
				}
				else if ( mappingElement instanceof EntityMapping<?> entityMapping ) {
					results.add( ResultMementoEntityJpa.from( entityMapping, factory ) );
				}
				else {
					throw new IllegalArgumentException( "Unsupported jakarta.persistence.sql.MappingElement type : " + resultSetMapping.getClass().getName() );
				}
			}
		}
		else {
			throw new IllegalArgumentException( "Unsupported jakarta.persistence.sql.ResultSetMapping type : " + resultSetMapping.getClass().getName() );
		}
		return new NamedResultSetMappingMementoImpl( null, results );
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
