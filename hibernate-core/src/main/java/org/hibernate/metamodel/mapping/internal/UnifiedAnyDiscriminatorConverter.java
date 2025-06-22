/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.HibernateException;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.internal.FullNameImplicitDiscriminatorStrategy;
import org.hibernate.metamodel.mapping.DiscriminatorConverter;
import org.hibernate.metamodel.mapping.DiscriminatorValueDetails;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.ImplicitDiscriminatorStrategy;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.java.CharacterJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.StringJavaType;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

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

		this.detailsByValue = CollectionHelper.concurrentMap( explicitValueMappings.size() );
		this.detailsByEntityName = CollectionHelper.concurrentMap( explicitValueMappings.size() );
		explicitValueMappings.forEach( (value,entityName) -> {
			final String importedEntityName = mappingMetamodel.getImportedName( entityName );
			final EntityPersister entityMapping = mappingMetamodel.getEntityDescriptor( importedEntityName );
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
		final DiscriminatorValueDetails details = new DiscriminatorValueDetailsImpl( value, entityMapping );
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

		final DiscriminatorValueDetails existing = detailsByValue.get( relationalValue );
		if ( existing != null ) {
			return existing;
		}

		if ( relationalValue.getClass().isEnum() ) {
			final Object enumValue;
			if ( getRelationalJavaType() instanceof StringJavaType ) {
				enumValue = ( (Enum<?>) relationalValue ).name();
			}
			else if ( getRelationalJavaType() instanceof CharacterJavaType ) {
				enumValue = ( (Enum<?>) relationalValue ).name().charAt( 0 );
			}
			else {
				enumValue = ( (Enum<?>) relationalValue ).ordinal();
			}
			final DiscriminatorValueDetails enumMatch = detailsByValue.get( enumValue );
			if ( enumMatch != null ) {
				return enumMatch;
			}
		}

		if ( implicitValueStrategy != null ) {
			final EntityMappingType entityMapping = implicitValueStrategy.toEntityMapping( relationalValue, discriminatorRole, mappingMetamodel );
			if ( entityMapping != null ) {
				return register( relationalValue, entityMapping );
			}
		}

		final DiscriminatorValueDetails nonNullMatch = detailsByValue.get( NOT_NULL_DISCRIMINATOR );
		if ( nonNullMatch != null ) {
			return nonNullMatch;
		}

		throw new HibernateException( "Unknown discriminator value (" + discriminatorRole.getFullPath() + ") : " + relationalValue );
	}

	@Override
	public DiscriminatorValueDetails getDetailsForEntityName(String entityName) {
		final DiscriminatorValueDetails existing = detailsByEntityName.get( entityName );
		if ( existing != null) {
			return existing;
		}

		if ( implicitValueStrategy != null ) {
			final EntityMappingType entityMapping = mappingMetamodel.getEntityDescriptor( entityName );
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
		for ( DiscriminatorValueDetails valueDetails : detailsByEntityName.values() ) {
			final X result = handler.apply( valueDetails );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}
}
