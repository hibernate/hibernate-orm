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
import jakarta.persistence.sql.MemberMapping;
import jakarta.persistence.sql.TupleMapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.query.results.spi.ResultSetMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static jakarta.persistence.sql.ResultSetMapping.embedded;
import static jakarta.persistence.sql.ResultSetMapping.entity;
import static jakarta.persistence.sql.ResultSetMapping.field;

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

	public static <T> jakarta.persistence.sql.ResultSetMapping<T> makeJpaMapping(
			Class<T> entityType,
			SessionFactoryImplementor sessionFactory) {
		var entityDescriptor = sessionFactory.getMappingMetamodel().getEntityDescriptor( entityType );
		final EntityDiscriminatorMapping discriminatorMapping = entityDescriptor.getDiscriminatorMapping();
		return entity(
				entityType,
				discriminatorMapping == null ? null : discriminatorMapping.getSelectableName(),
				collectMemberMappings( entityType, entityDescriptor, sessionFactory )
		);
	}

	private static <T> MemberMapping<T>[] collectMemberMappings(
			Class<T> entityType,
			EntityMappingType entityTypeDescriptor,
			SessionFactoryImplementor sessionFactory) {
		final List<MemberMapping<T>> memberMappingList = new ArrayList<>();
		applyAttributeMapping( entityType, entityTypeDescriptor, memberMappingList::add, sessionFactory );

		//noinspection unchecked
		return memberMappingList.toArray(new MemberMapping[]{});
	}

	private static <T> void applyAttributeMapping(
			Class<T> entityType,
			EntityMappingType entityTypeDescriptor,
			Consumer<MemberMapping<T>> memberMappingConsumer,
			SessionFactoryImplementor sessionFactory) {
		final EntityIdentifierMapping identifierMapping = entityTypeDescriptor.getIdentifierMapping();
		if ( identifierMapping instanceof SingleAttributeIdentifierMapping idAttribute ) {
			applyAttributeMapping( entityType, idAttribute, memberMappingConsumer, sessionFactory );
		}
		else {
			( (NonAggregatedIdentifierMapping) identifierMapping ).getVirtualIdEmbeddable().forEachAttributeMapping( (idAttribute) -> {
				applyAttributeMapping( entityType, idAttribute, memberMappingConsumer, sessionFactory );
			} );
		}

		applyAttributeMapping( entityType, (ManagedMappingType) entityTypeDescriptor, memberMappingConsumer, sessionFactory );
	}

	private static <T> MemberMapping<T>[] collectMemberMappings(
			Class<T> managedType,
			ManagedMappingType managedTypeDescriptor,
			SessionFactoryImplementor sessionFactory) {
		final List<MemberMapping<T>> memberMappingList = new ArrayList<>();
		applyAttributeMapping( managedType, managedTypeDescriptor, memberMappingList::add, sessionFactory );

		//noinspection unchecked
		return memberMappingList.toArray(new MemberMapping[]{});
	}

	private static <T> void applyAttributeMapping(
			Class<T> managedType,
			ManagedMappingType managedTypeDescriptor,
			Consumer<MemberMapping<T>> memberMappingConsumer,
			SessionFactoryImplementor sessionFactory) {
		managedTypeDescriptor.forEachAttributeMapping( (attribute) -> {
			applyAttributeMapping( managedType, attribute, memberMappingConsumer, sessionFactory );
		} );
	}

	private static <T,A> void applyAttributeMapping(
			Class<T> containerType,
			AttributeMapping attributeDescriptor,
			Consumer<MemberMapping<T>> memberMappingConsumer,
			SessionFactoryImplementor sessionFactory) {
		if ( attributeDescriptor instanceof BasicValuedModelPart basicAttribute ) {
			memberMappingConsumer.accept( field(
					containerType,
					basicAttribute.getJavaType().getJavaTypeClass(),
					basicAttribute.getFetchableName(),
					basicAttribute.getSelectableName()
			) );
		}
		else if ( attributeDescriptor instanceof EmbeddableValuedModelPart embeddedAttribute ) {
			//noinspection unchecked
			Class<A> embeddedJavaType = (Class<A>) embeddedAttribute.getEmbeddableTypeDescriptor()
					.getJavaType()
					.getJavaTypeClass();
			memberMappingConsumer.accept( embedded(
					containerType,
					embeddedJavaType,
					embeddedAttribute.getFetchableName(),
					collectMemberMappings(
							embeddedJavaType,
							embeddedAttribute.getEmbeddableTypeDescriptor(),
							sessionFactory
					)
			) );
		}
	}
}
