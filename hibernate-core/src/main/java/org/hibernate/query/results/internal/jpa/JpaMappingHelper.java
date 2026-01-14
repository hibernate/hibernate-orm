/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.jpa;

import jakarta.persistence.sql.ColumnMapping;
import jakarta.persistence.sql.CompoundMapping;
import jakarta.persistence.sql.ConstructorMapping;
import jakarta.persistence.sql.EntityMapping;
import jakarta.persistence.sql.MappingElement;
import jakarta.persistence.sql.TupleMapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.results.spi.ResultSetMapping;

/// Support for dealing with Jakarta Persistence [jakarta.persistence.sql.ResultSetMapping], both
/// in terms of -
///
/// * converting one to the Hibernate [form][ResultSetMapping]
/// * creating one from Hibernate's [memento][org.hibernate.query.named.NamedResultSetMappingMemento]
///
/// @author Steve Ebersole
public class JpaMappingHelper {
	public static <T> ResultSetMapping toHibernateMapping(
			jakarta.persistence.sql.ResultSetMapping<T> jpaMapping,
			SessionFactoryImplementor sessionFactory) {
		var resultMapping = sessionFactory.getJdbcValuesMappingProducerProvider()
				.buildResultSetMapping( null, true, sessionFactory );
		apply( jpaMapping, resultMapping, sessionFactory );
		return resultMapping;
	}

	private static <T> void apply(
			jakarta.persistence.sql.ResultSetMapping<T> jpaMapping,
			ResultSetMapping resultMapping,
			SessionFactoryImplementor sessionFactory) {
		if ( jpaMapping instanceof ColumnMapping<T> columnMapping ) {
			resultMapping.addResultBuilder( new ColumnBuilder<>( columnMapping, sessionFactory ) );
		}
		else if ( jpaMapping instanceof ConstructorMapping<T> constructorMapping ) {
			resultMapping.addResultBuilder( new ConstructorBuilder<>( constructorMapping, sessionFactory ) );
		}
		else if ( jpaMapping instanceof EntityMapping<T> entityMapping ) {
			resultMapping.addResultBuilder( new EntityBuilder<>( entityMapping, sessionFactory ) );
		}
		else if ( jpaMapping instanceof TupleMapping tupleMapping ) {
			resultMapping.addResultBuilder( new TupleBuilder( tupleMapping, sessionFactory ) );
		}
		else if ( jpaMapping instanceof CompoundMapping compoundMapping ) {
			for ( int i = 0; i < compoundMapping.elements().length; i++ ) {
				final MappingElement<?> mappingElement = compoundMapping.elements()[i];
				resultMapping.addResultBuilder( toHibernateBuilder( mappingElement, sessionFactory ) );
			}
		}
		else {
			throw new IllegalArgumentException( "Unsupported jakarta.persistence.sql.ResultSetMapping type : " + jpaMapping.getClass().getName() );
		}
	}

	public static <T> MappingElementBuilder<T> toHibernateBuilder(
			jakarta.persistence.sql.MappingElement<T> jpaMapping,
			SessionFactoryImplementor sessionFactory) {
		if ( jpaMapping instanceof ColumnMapping<T> columnMapping ) {
			return new ColumnBuilder<>( columnMapping, sessionFactory );
		}
		else if ( jpaMapping instanceof ConstructorMapping<T> constructorMapping ) {
			return new ConstructorBuilder<>( constructorMapping, sessionFactory );
		}
		else if ( jpaMapping instanceof EntityMapping<T> entityMapping ) {
			return new EntityBuilder<>( entityMapping, sessionFactory );
		}
		else {
			throw new IllegalArgumentException( "Unsupported jakarta.persistence.sql.MappingElement type : " + jpaMapping.getClass().getName() );
		}
	}
}
