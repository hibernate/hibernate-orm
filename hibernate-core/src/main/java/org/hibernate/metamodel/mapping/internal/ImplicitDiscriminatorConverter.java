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
import org.hibernate.type.descriptor.java.JavaType;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Locale.ROOT;

/**
 * @author Steve Ebersole
 */
public class ImplicitDiscriminatorConverter<O,R> extends DiscriminatorConverter<O,R> {
	private final NavigableRole discriminatorRole;
	private final MappingMetamodelImplementor mappingMetamodel;
	private final Map<Object, DiscriminatorValueDetails> detailsByValue;
	private final Map<String,DiscriminatorValueDetails> detailsByEntityName;

	public ImplicitDiscriminatorConverter(
			NavigableRole discriminatorRole,
			JavaType<O> domainJavaType,
			JavaType<R> relationalJavaType,
			Map<Object,String> explicitValueMappings,
			MappingMetamodelImplementor mappingMetamodel) {
		super( discriminatorRole.getFullPath(), domainJavaType, relationalJavaType );
		this.discriminatorRole = discriminatorRole;
		this.mappingMetamodel = mappingMetamodel;

		if ( CollectionHelper.isNotEmpty( explicitValueMappings ) ) {
			throw new MappingException( String.format(
					ROOT,
					"Encountered explicit ANY discriminator mappings (%s)",
					discriminatorRole.getFullPath()
			) );
		}

		this.detailsByValue = CollectionHelper.concurrentMap( 8 );
		this.detailsByEntityName = CollectionHelper.concurrentMap( 8 );
	}

	@Override
	public DiscriminatorValueDetails getDetailsForDiscriminatorValue(Object value) {
		if ( value instanceof String incoming ) {
			final DiscriminatorValueDetails existingDetails = detailsByValue.get( incoming );
			if ( existingDetails != null ) {
				return existingDetails;
			}
			final String entityName = mappingMetamodel.getImportedName( incoming );
			final EntityPersister persister = mappingMetamodel.findEntityDescriptor( entityName );
			if ( persister != null ) {
				assert persister.getImportedName().equals( incoming );
				return register( incoming, persister );
			}
		}
		throw new HibernateException( String.format(
				ROOT,
				"Unrecognized discriminator value (%s): %s",
				discriminatorRole.getFullPath(),
				value
		) );
	}

	private DiscriminatorValueDetails register(Object value, EntityPersister entityDescriptor) {
		final DiscriminatorValueDetails details = new DiscriminatorValueDetailsImpl( value, entityDescriptor );
		detailsByValue.put( value, details );
		detailsByEntityName.put( entityDescriptor.getImportedName(), details );
		return details;
	}

	@Override
	public DiscriminatorValueDetails getDetailsForEntityName(String entityName) {
		final DiscriminatorValueDetails existingDetails = detailsByEntityName.get( entityName );
		if ( existingDetails != null ) {
			return existingDetails;
		}
		final EntityPersister persister = mappingMetamodel.findEntityDescriptor( entityName );
		if ( persister!= null ) {
			return register( persister.getImportedName(), persister );
		}
		throw new HibernateException( String.format(
				ROOT,
				"Unrecognized entity name (%s): %s",
				discriminatorRole.getFullPath(),
				entityName
		) );
	}

	@Override
	public void forEachValueDetail(Consumer<DiscriminatorValueDetails> consumer) {
	}

	@Override
	public <X> X fromValueDetails(Function<DiscriminatorValueDetails,X> handler) {
		return null;
	}
}
