/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
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

import static java.util.Locale.ROOT;
import static org.hibernate.persister.entity.DiscriminatorHelper.NOT_NULL_DISCRIMINATOR;
import static org.hibernate.persister.entity.DiscriminatorHelper.NULL_DISCRIMINATOR;

/**
 * @author Steve Ebersole
 */
public class ExplicitDiscriminatorConverter<O,R> extends DiscriminatorConverter<O, R> {
	private final NavigableRole discriminatorRole;
	private final Map<Object, DiscriminatorValueDetails> detailsByValue;
	private final Map<String,DiscriminatorValueDetails> detailsByEntityName;

	public ExplicitDiscriminatorConverter(
			NavigableRole discriminatorRole,
			JavaType<O> domainJavaType,
			JavaType<R> relationalJavaType,
			Map<Object, String> explicitValueMappings,
			MappingMetamodelImplementor mappingMetamodel) {
		super( discriminatorRole.getFullPath(), domainJavaType, relationalJavaType );
		this.discriminatorRole = discriminatorRole;

		if ( CollectionHelper.isEmpty( explicitValueMappings ) ) {
			throw new MappingException( String.format(
					ROOT,
					"No explicit ANY discriminator mappings (%s)",
					discriminatorRole.getFullPath()
			) );
		}

		this.detailsByValue = CollectionHelper.concurrentMap( explicitValueMappings.size() );
		this.detailsByEntityName = CollectionHelper.concurrentMap( explicitValueMappings.size() );

		explicitValueMappings.forEach( (value, entityName) -> {
			final EntityPersister entityDescriptor = mappingMetamodel.getEntityDescriptor( entityName );
			final DiscriminatorValueDetails details = new DiscriminatorValueDetailsImpl( value, entityDescriptor );
			detailsByValue.put( value, details );
			detailsByEntityName.put( entityDescriptor.getEntityName(), details );
		} );
	}

	@Override
	public AnyDiscriminatorValueStrategy getValueStrategy() {
		return AnyDiscriminatorValueStrategy.EXPLICIT;
	}

	public Map<Object, DiscriminatorValueDetails> getDetailsByValue() {
		return detailsByValue;
	}

	public Map<String, DiscriminatorValueDetails> getDetailsByEntityName() {
		return detailsByEntityName;
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
				enumValue = ( (Enum<?>) relationalForm ).name();
			}
			else if ( getRelationalJavaType() instanceof CharacterJavaType ) {
				enumValue = ( (Enum<?>) relationalForm ).name().charAt( 0 );
			}
			else {
				enumValue = ( (Enum<?>) relationalForm ).ordinal();
			}
			final DiscriminatorValueDetails enumMatch = detailsByValue.get( enumValue );
			if ( enumMatch != null ) {
				return enumMatch;
			}
		}

		throw new HibernateException( String.format(
				ROOT,
				"Unknown discriminator value (%s) : %s",
				discriminatorRole,
				relationalForm
		) );
	}

	@Override
	public DiscriminatorValueDetails getDetailsForEntityName(String entityName) {
		final DiscriminatorValueDetails valueDetails = detailsByEntityName.get( entityName );
		if ( valueDetails != null) {
			return valueDetails;
		}
		throw new HibernateException( "Entity not explicitly mapped for ANY discriminator (" + discriminatorRole + ") : " + entityName );
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
