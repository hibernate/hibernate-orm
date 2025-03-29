/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.revisioninfo;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.service.ServiceRegistry;

/**
 * Automatically adds entity names, that have been changed during current revision, to revision entity.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 *
 * @see org.hibernate.envers.ModifiedEntityNames
 * @see org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity
 */
public class DefaultTrackingModifiedEntitiesRevisionInfoGenerator extends DefaultRevisionInfoGenerator {
	private final Setter modifiedEntityNamesSetter;
	private final Getter modifiedEntityNamesGetter;

	public DefaultTrackingModifiedEntitiesRevisionInfoGenerator(
			String revisionInfoEntityName,
			Class<?> revisionInfoClass,
			Class<? extends RevisionListener> listenerClass,
			RevisionTimestampValueResolver timestampValueResolver,
			PropertyData modifiedEntityNamesData,
			ServiceRegistry serviceRegistry) {
		super( revisionInfoEntityName, revisionInfoClass, listenerClass, timestampValueResolver, serviceRegistry );
		modifiedEntityNamesSetter = ReflectionTools.getSetter( revisionInfoClass, modifiedEntityNamesData, serviceRegistry );
		modifiedEntityNamesGetter = ReflectionTools.getGetter( revisionInfoClass, modifiedEntityNamesData, serviceRegistry );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void entityChanged(
			Class entityClass,
			String entityName,
			Object entityId,
			RevisionType revisionType,
			Object revisionEntity) {
		super.entityChanged( entityClass, entityName, entityId, revisionType, revisionEntity );
		Set<String> modifiedEntityNames = (Set<String>) modifiedEntityNamesGetter.get( revisionEntity );
		if ( modifiedEntityNames == null ) {
			modifiedEntityNames = new HashSet<>();
			modifiedEntityNamesSetter.set( revisionEntity, modifiedEntityNames );
		}
		modifiedEntityNames.add( entityName );
	}
}
