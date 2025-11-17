/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.FetchProfileOverride;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.PersistentClass;

import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.mapping.MetadataSource.ANNOTATIONS;

/**
 * @author Gavin King
 */
public class FetchSecondPass implements SecondPass {
	private final FetchProfileOverride fetch;
	private final PropertyHolder propertyHolder;
	private final String propertyName;
	private final MetadataBuildingContext buildingContext;

	public FetchSecondPass(
			FetchProfileOverride fetch,
			PropertyHolder propertyHolder,
			String propertyName,
			MetadataBuildingContext buildingContext) {
		this.fetch = fetch;
		this.propertyHolder = propertyHolder;
		this.propertyName = propertyName;
		this.buildingContext = buildingContext;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		final String profileName = fetch.profile();
		final var profile = buildingContext.getMetadataCollector().getFetchProfile( profileName );
		if ( profile == null ) {
			throw new AnnotationException(
					"Property '" + qualify( propertyHolder.getPath(), propertyName )
							+ "' refers to an unknown fetch profile named '" + profileName + "'"
			);
		}

		if ( profile.getSource() == ANNOTATIONS ) {
			profile.addFetch( new FetchProfile.Fetch(
					propertyHolder.getEntityName(),
					propertyName,
					fetch.mode(),
					fetch.fetch()
			) );
		}
		// otherwise, it's a fetch profile defined in XML, and it overrides
		// the annotations, so we simply ignore this annotation completely
	}
}
