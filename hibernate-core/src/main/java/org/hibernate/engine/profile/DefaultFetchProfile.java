/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.profile;

import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.results.graph.FetchOptions;

import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.engine.FetchStyle.SUBSELECT;
import static org.hibernate.engine.FetchTiming.IMMEDIATE;
import static org.hibernate.engine.FetchStyle.JOIN;

/**
 * @author Gavin King
 */
public class DefaultFetchProfile extends FetchProfile {
	/**
	 * The name of an implicit fetch profile which includes all eager to-one associations.
	 */
	public static final String HIBERNATE_DEFAULT_PROFILE = "org.hibernate.defaultProfile";
	private final MappingMetamodel metamodels;

	public DefaultFetchProfile(MappingMetamodel metamodels) {
		super(HIBERNATE_DEFAULT_PROFILE);
		this.metamodels = metamodels;
	}

	@Override
	public @Nullable Fetch getFetchByRole(String role) {
		final int last = role.lastIndexOf('.');
		final String entityName = role.substring( 0, last );
		final String property = role.substring( last + 1 );
		final EntityMappingType entity = metamodels.getEntityDescriptor( entityName );
		if ( entity != null ) {
			final AttributeMapping attributeMapping = entity.findAttributeMapping( property );
			if ( attributeMapping != null && !attributeMapping.isPluralAttributeMapping() ) {
				final FetchOptions fetchOptions = attributeMapping.getMappedFetchOptions();
				if ( fetchOptions.getStyle() == JOIN && fetchOptions.getTiming() == IMMEDIATE ) {
					return new Fetch( new Association( entity.getEntityPersister(), role ), JOIN, IMMEDIATE );
				}
			}
		}
		return super.getFetchByRole( role );
	}

	@Override
	public boolean hasSubselectLoadableCollectionsEnabled(EntityPersister persister) {
		final AttributeMappingsList attributeMappings = persister.getAttributeMappings();
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			AttributeMapping attributeMapping = attributeMappings.get( i );
			if ( attributeMapping.getMappedFetchOptions().getStyle() == SUBSELECT ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Map<String, Fetch> getFetches() {
		throw new UnsupportedOperationException( "DefaultFetchProfile has implicit fetches" );
	}
}
