/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.internal.FullNameImplicitDiscriminatorStrategy;
import org.hibernate.metamodel.mapping.DiscriminatorConverter;
import org.hibernate.metamodel.mapping.DiscriminatorValueDetails;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.ImplicitDiscriminatorStrategy;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.type.descriptor.java.CharacterJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.StringJavaType;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.hibernate.Hibernate.unproxy;
import static org.hibernate.internal.util.collections.CollectionHelper.concurrentMap;
import static org.hibernate.persister.entity.DiscriminatorHelper.NOT_NULL_DISCRIMINATOR;
import static org.hibernate.persister.entity.DiscriminatorHelper.NULL_DISCRIMINATOR;

/**
 * @author Steve Ebersole
 */
public class UnifiedAnyDiscriminatorConverter<O,R> extends DiscriminatorConverter<O,R> {
	private final NavigableRole discriminatorRole;
	private final Map<Object, DiscriminatorValueDetails> detailsByValue;
	private final Map<String,DiscriminatorValueDetails> detailsByEntityName;
	private final ImplicitDiscriminatorStrategy implicitValueStrategy;
	private final MappingMetamodelImplementor mappingMetamodel;

	public UnifiedAnyDiscriminatorConverter(
			NavigableRole discriminatorRole,
			JavaType<O> domainJavaType,
			JavaType<R> relationalJavaType,
			Map<Object,String> explicitValueMappings,
			ImplicitDiscriminatorStrategy implicitValueStrategy,
			MappingMetamodelImplementor mappingMetamodel) {
		super( discriminatorRole.getFullPath(), domainJavaType, relationalJavaType );
		this.discriminatorRole = discriminatorRole;
		this.mappingMetamodel = mappingMetamodel;

		this.implicitValueStrategy = resolveImplicitValueStrategy( implicitValueStrategy, explicitValueMappings );

		detailsByValue = concurrentMap( explicitValueMappings.size() );
		detailsByEntityName = concurrentMap( explicitValueMappings.size() );
		explicitValueMappings.forEach( (value,entityName) -> {
			final String importedEntityName = mappingMetamodel.getImportedName( entityName );
			final var entityMapping = mappingMetamodel.getEntityDescriptor( importedEntityName );
			register( value, entityMapping );
		} );
	}

	private ImplicitDiscriminatorStrategy resolveImplicitValueStrategy(ImplicitDiscriminatorStrategy implicitValueStrategy, Map<Object, String> explicitValueMappings) {
		if ( explicitValueMappings.isEmpty() ) {
			if ( implicitValueStrategy == null ) {
				return FullNameImplicitDiscriminatorStrategy.FULL_NAME_STRATEGY;
			}
		}
		else {
			if ( explicitValueMappings.containsKey( NOT_NULL_DISCRIMINATOR ) ) {
				if ( implicitValueStrategy != null ) {
					// we will ultimately not know how to handle "implicit" values which are non-null
					throw new HibernateException( "Illegal use of ImplicitDiscriminatorStrategy with explicit non-null discriminator mapping: " + discriminatorRole.getFullPath() );
				}
			}
		}
		return implicitValueStrategy;
	}

	private DiscriminatorValueDetails register(Object value, EntityMappingType entityMapping) {
		final var details = new DiscriminatorValueDetailsImpl( value, entityMapping );
		detailsByValue.put( value, details );
		detailsByEntityName.put( entityMapping.getEntityName(), details );
		return details;
	}

	public Map<Object, DiscriminatorValueDetails> getDetailsByValue() {
		return detailsByValue;
	}

	public Map<String, DiscriminatorValueDetails> getDetailsByEntityName() {
		return detailsByEntityName;
	}

	@Override
	public DiscriminatorValueDetails getDetailsForDiscriminatorValue(Object relationalValue) {
		if ( relationalValue == null ) {
			return detailsByValue.get( NULL_DISCRIMINATOR );
		}

		final var existing = detailsByValue.get( relationalValue );
		if ( existing != null ) {
			return existing;
		}

		if ( relationalValue.getClass().isEnum() ) {
			final Object enumValue = enumValue( (Enum<?>) relationalValue );
			final var enumMatch = detailsByValue.get( enumValue );
			if ( enumMatch != null ) {
				return enumMatch;
			}
		}

		if ( implicitValueStrategy != null ) {
			final var entityMapping =
					implicitValueStrategy.toEntityMapping( relationalValue, discriminatorRole, mappingMetamodel );
			if ( entityMapping != null ) {
				return register( relationalValue, entityMapping );
			}
		}

		final var nonNullMatch = detailsByValue.get( NOT_NULL_DISCRIMINATOR );
		if ( nonNullMatch != null ) {
			return nonNullMatch;
		}

		throw new HibernateException( "Unknown discriminator value (" + discriminatorRole.getFullPath() + ") : " + relationalValue );
	}

	private Object enumValue(Enum<?> relationalEnum) {
		final var relationalJavaType = getRelationalJavaType();
		if ( relationalJavaType instanceof StringJavaType ) {
			return relationalEnum.name();
		}
		else if ( relationalJavaType instanceof CharacterJavaType ) {
			return relationalEnum.name().charAt( 0 );
		}
		else {
			return relationalEnum.ordinal();
		}
	}

	@Override
	public DiscriminatorValueDetails getDetailsForEntityName(String entityName) {
		final var existing = detailsByEntityName.get( entityName );
		if ( existing != null) {
			return existing;
		}

		if ( implicitValueStrategy != null ) {
			final var entityMapping = mappingMetamodel.getEntityDescriptor( entityName );
			assert entityMapping != null;
			final Object discriminatorValue = implicitValueStrategy.toDiscriminatorValue(
					entityMapping,
					discriminatorRole,
					mappingMetamodel
			);
			return register( discriminatorValue, entityMapping );
		}

		throw new HibernateException( "Cannot determine discriminator value from entity-name (" + discriminatorRole.getFullPath() + ") : " + entityName );
	}

	@Override
	public void forEachValueDetail(Consumer<DiscriminatorValueDetails> consumer) {
		detailsByEntityName.values().forEach( consumer );
	}

	@Override
	public <X> X fromValueDetails(Function<DiscriminatorValueDetails, X> handler) {
		for ( var valueDetails : detailsByEntityName.values() ) {
			final X result = handler.apply( valueDetails );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	@Override
	protected String getEntityName(O domainForm) {
		final Class<?> entityClass;
		if ( domainForm == null ) {
			return null;
		}
		else if ( domainForm instanceof Class<?> clazz ) {
			entityClass = clazz;
		}
		else if ( domainForm instanceof String name ) {
			return name;
		}
		else {
			entityClass = unproxy( domainForm ).getClass();
		}
		try {
			return mappingMetamodel.getEntityDescriptor( entityClass ).getEntityName();
		}
		catch (IllegalArgumentException iae) {
			throw new IllegalArgumentException( "Illegal discriminator value: " + domainForm );
		}
	}
}
