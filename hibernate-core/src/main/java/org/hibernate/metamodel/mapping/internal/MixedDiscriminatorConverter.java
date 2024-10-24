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
import org.hibernate.type.AnyDiscriminatorValueStrategy;
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
	private final boolean implicitEntityShortName;
	private final MappingMetamodelImplementor mappingMetamodel;

	public MixedDiscriminatorConverter(
			NavigableRole discriminatorRole,
			JavaType<O> domainJavaType,
			JavaType<R> relationalJavaType,
			Map<Object,String> explicitValueMappings,
			boolean implicitEntityShortName,
			MappingMetamodelImplementor mappingMetamodel) {
		super( discriminatorRole.getFullPath(), domainJavaType, relationalJavaType );
		this.discriminatorRole = discriminatorRole;
		this.implicitEntityShortName = implicitEntityShortName;
		this.mappingMetamodel = mappingMetamodel;

		this.detailsByValue = CollectionHelper.concurrentMap( explicitValueMappings.size() );
		this.detailsByEntityName = CollectionHelper.concurrentMap( explicitValueMappings.size() );
		explicitValueMappings.forEach( (value,entityName) -> {
			final String importedEntityName = mappingMetamodel.getImportedName( entityName );
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
	public AnyDiscriminatorValueStrategy getValueStrategy() {
		return AnyDiscriminatorValueStrategy.MIXED;
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

		if ( relationalValue instanceof String assumedEntityName ) {
			final EntityPersister persister;
			if ( implicitEntityShortName ) {
				final String importedName = mappingMetamodel.getImportedName( assumedEntityName );
				persister = mappingMetamodel.findEntityDescriptor( importedName );
			}
			else {
				persister = mappingMetamodel.findEntityDescriptor( assumedEntityName );
			}

			if ( persister != null ) {
				return register( assumedEntityName, persister );
			}
		}

		final DiscriminatorValueDetails notNullMatch = detailsByValue.get( NOT_NULL_DISCRIMINATOR );
		if ( notNullMatch != null ) {
			return notNullMatch;
		}

		throw new HibernateException( "Cannot interpret discriminator value (" + discriminatorRole + ") : " + relationalValue );
	}

	@Override
	public DiscriminatorValueDetails getDetailsForEntityName(String entityName) {
		final DiscriminatorValueDetails existing = detailsByEntityName.get( entityName );
		if ( existing != null) {
			return existing;
		}

		final EntityPersister entityDescriptor = mappingMetamodel.getEntityDescriptor( entityName );
		final String implicitName = implicitEntityShortName ? entityDescriptor.getImportedName() : entityName;
		return register( implicitName, entityDescriptor );
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
