/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.internal.revisioninfo;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;

/**
 * Automatically adds entity names, that have been changed during current revision, to revision entity.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 *
 * @see org.hibernate.envers.ModifiedEntityNames
 * @see org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity
 */
public class DefaultTrackingModifiedEntitiesRevisionInfoGenerator extends DefaultRevisionInfoGenerator {
	private final Setter modifiedEntityNamesSetter;
	private final Getter modifiedEntityNamesGetter;

	public DefaultTrackingModifiedEntitiesRevisionInfoGenerator(
			String revisionInfoEntityName, Class<?> revisionInfoClass,
			Class<? extends RevisionListener> listenerClass,
			PropertyData revisionInfoTimestampData, boolean timestampAsDate,
			PropertyData modifiedEntityNamesData) {
		super( revisionInfoEntityName, revisionInfoClass, listenerClass, revisionInfoTimestampData, timestampAsDate );
		modifiedEntityNamesSetter = ReflectionTools.getSetter( revisionInfoClass, modifiedEntityNamesData );
		modifiedEntityNamesGetter = ReflectionTools.getGetter( revisionInfoClass, modifiedEntityNamesData );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public void entityChanged(
			Class entityClass,
			String entityName,
			Serializable entityId,
			RevisionType revisionType,
			Object revisionEntity) {
		super.entityChanged( entityClass, entityName, entityId, revisionType, revisionEntity );
		Set<String> modifiedEntityNames = (Set<String>) modifiedEntityNamesGetter.get( revisionEntity );
		if ( modifiedEntityNames == null ) {
			modifiedEntityNames = new HashSet<String>();
			modifiedEntityNamesSetter.set( revisionEntity, modifiedEntityNames, null );
		}
		modifiedEntityNames.add( entityName );
	}
}
