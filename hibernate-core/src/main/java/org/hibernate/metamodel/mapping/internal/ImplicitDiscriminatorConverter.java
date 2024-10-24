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
	private final boolean implicitEntityShortName;
	private final MappingMetamodelImplementor mappingMetamodel;
	private final Map<Object, DiscriminatorValueDetails> detailsByValue;
	private final Map<String,DiscriminatorValueDetails> detailsByEntityName;

	public ImplicitDiscriminatorConverter(
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
	public AnyDiscriminatorValueStrategy getValueStrategy() {
		return AnyDiscriminatorValueStrategy.IMPLICIT;
	}

	public Map<Object, DiscriminatorValueDetails> getDetailsByValue() {
		return detailsByValue;
	}

	public Map<String, DiscriminatorValueDetails> getDetailsByEntityName() {
		return detailsByEntityName;
	}

	@Override
	public DiscriminatorValueDetails getDetailsForDiscriminatorValue(Object relationalValue) {
		// entity-name : org.hibernate.Thing
		// short-name : Thing

		// in the case of full entity-name handling, we'd have
		// 		detailsByValue["org.hibernate.Thing", DiscriminatorValueDetails( "org.hibernate.Thing", ... )]
		//		detailsByEntityName["org.hibernate.Thing", DiscriminatorValueDetails( "org.hibernate.Thing", ... )]

		// in the case of short-name "Thing", we'd have
		// 		detailsByValue["Thing", DiscriminatorValueDetails( "Thing", ... )]
		//		detailsByEntityName["org.hibernate.Thing", DiscriminatorValueDetails( "Thing", ... )]

		final DiscriminatorValueDetails existing = detailsByValue.get( relationalValue );
		if ( existing != null ) {
			return existing;
		}

		if ( relationalValue instanceof String incoming ) {
			final EntityPersister persister;
			if ( implicitEntityShortName ) {
				// incoming - "Thing"
				// importedName - "org.hibernate.Thing"
				final String importedName = mappingMetamodel.getImportedName( incoming );
				persister = mappingMetamodel.findEntityDescriptor( importedName );
			}
			else {
				// incoming - "org.hibernate.Thing"
				// importedName - "org.hibernate.Thing"
				persister = mappingMetamodel.findEntityDescriptor( incoming );
			}

			if ( persister != null ) {
				return register( incoming, persister );
			}
		}

		throw new HibernateException( String.format(
				ROOT,
				"Unrecognized discriminator relationalValue (%s): %s",
				discriminatorRole.getFullPath(),
				relationalValue
		) );
	}

	private DiscriminatorValueDetails register(Object value, EntityPersister entityDescriptor) {
		final DiscriminatorValueDetails details = new DiscriminatorValueDetailsImpl( value, entityDescriptor );
		detailsByValue.put( value, details );
		detailsByEntityName.put( entityDescriptor.getEntityName(), details );
		return details;
	}

	@Override
	public DiscriminatorValueDetails getDetailsForEntityName(String entityName) {
		// entityName - "org.hibernate.Thing"

		final DiscriminatorValueDetails existingDetails = detailsByEntityName.get( entityName );
		if ( existingDetails != null ) {
			return existingDetails;
		}

		final EntityPersister persister = mappingMetamodel.findEntityDescriptor( entityName );
		if ( persister!= null ) {
			final String implicitValue = implicitEntityShortName ? persister.getImportedName() : persister.getEntityName();
			return register( implicitValue, persister );
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
