/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.HibernateException;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.DiscriminatorConverter;
import org.hibernate.metamodel.mapping.DiscriminatorValueDetails;
import org.hibernate.metamodel.model.domain.NavigableRole;
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
 * DiscriminatorConverter for use with {@linkplain org.hibernate.type.AnyDiscriminatorValueStrategy#MIXED}
 *
 * @author Steve Ebersole
 */
public class MixedDiscriminatorConverter<O,R> extends DiscriminatorConverter<O,R> {
	private final NavigableRole discriminatorRole;
	private final Map<Object, DiscriminatorValueDetails> detailsByValue;
	private final Map<String,DiscriminatorValueDetails> detailsByEntityName;
	private final MappingMetamodelImplementor mappingMetamodel;

	public MixedDiscriminatorConverter(
			NavigableRole discriminatorRole,
			JavaType<O> domainJavaType,
			JavaType<R> relationalJavaType,
			Map<Object,String> explicitValueMappings,
			MappingMetamodelImplementor mappingMetamodel) {
		super( discriminatorRole.getFullPath(), domainJavaType, relationalJavaType );
		this.discriminatorRole = discriminatorRole;
		this.mappingMetamodel = mappingMetamodel;

		this.detailsByValue = CollectionHelper.concurrentMap( explicitValueMappings.size() );
		this.detailsByEntityName = CollectionHelper.concurrentMap( explicitValueMappings.size() );
		explicitValueMappings.forEach( (value,entityName) -> {
			String importedEntityName = mappingMetamodel.getImportedName( entityName );
			final EntityPersister entityDescriptor = mappingMetamodel.getEntityDescriptor( importedEntityName );
			register( value, entityDescriptor );
		} );
	}

	private DiscriminatorValueDetails register(Object value, EntityPersister entityDescriptor) {
		final DiscriminatorValueDetails details = new DiscriminatorValueDetailsImpl( value, entityDescriptor );
		detailsByValue.put( value, details );
		detailsByEntityName.put( entityDescriptor.getEntityName(), details );
		return details;
	}

	@Override
	public DiscriminatorValueDetails getDetailsForDiscriminatorValue(Object relationalForm) {
		if ( relationalForm == null ) {
			return detailsByValue.get( NULL_DISCRIMINATOR );
		}

		final DiscriminatorValueDetails existing = detailsByValue.get( relationalForm );
		if ( existing != null ) {
			// an explicit or previously-resolved mapping
			return existing;
		}

		final DiscriminatorValueDetails notNullMatch = detailsByValue.get( NOT_NULL_DISCRIMINATOR );
		if ( notNullMatch != null ) {
			return notNullMatch;
		}

		if ( relationalForm.getClass().isEnum() ) {
			final Object enumValue;
			if ( getRelationalJavaType() instanceof StringJavaType ) {
				enumValue = ( (Enum) relationalForm ).name();
			}
			else if ( getRelationalJavaType() instanceof CharacterJavaType ) {
				enumValue = ( (Enum) relationalForm ).name().charAt( 0 );
			}
			else {
				enumValue = ( (Enum) relationalForm ).ordinal();
			}
			final DiscriminatorValueDetails enumMatch = detailsByValue.get( enumValue );
			if ( enumMatch != null ) {
				return enumMatch;
			}
		}

		if ( relationalForm instanceof String assumedEntityName ) {
			// Assume the relational form is the entity name
			final EntityPersister persister = mappingMetamodel.findEntityDescriptor( assumedEntityName );
			if ( persister != null ) {
				return register( assumedEntityName, persister );
			}
		}

		throw new HibernateException( "Cannot interpret discriminator value (" + discriminatorRole + ") : " + relationalForm );
	}

	@Override
	public DiscriminatorValueDetails getDetailsForEntityName(String entityName) {
		final DiscriminatorValueDetails existing = detailsByEntityName.get( entityName );
		if ( existing != null) {
			return existing;
		}

		final EntityPersister entityDescriptor = mappingMetamodel.getEntityDescriptor( entityName );
		return register( entityName, entityDescriptor );
	}

	@Override
	public void forEachValueDetail(Consumer<DiscriminatorValueDetails> consumer) {
		detailsByEntityName.forEach( (value, detail) -> consumer.accept( detail ) );
	}

	@Override
	public <X> X fromValueDetails(Function<DiscriminatorValueDetails, X> handler) {
		for ( DiscriminatorValueDetails detail : detailsByEntityName.values() ) {
			final X result = handler.apply( detail );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}
}
