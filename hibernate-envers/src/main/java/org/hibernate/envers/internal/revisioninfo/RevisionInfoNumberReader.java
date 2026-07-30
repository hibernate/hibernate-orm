/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.revisioninfo;

import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.property.access.spi.PropertyValueAccessor;
import org.hibernate.service.ServiceRegistry;

/**
 * Gets a revision number from a persisted revision info entity.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class RevisionInfoNumberReader {
	private final PropertyValueAccessor revisionIdGetter;

	public RevisionInfoNumberReader(Class<?> revisionInfoClass, PropertyData revisionInfoIdData, ServiceRegistry serviceRegistry, RevisionInfoGenerator generator) {
		revisionIdGetter = ReflectionTools.getPropertyValueAccessor( revisionInfoClass, revisionInfoIdData, serviceRegistry );
		generator.setRevisionInfoNumberReader( this );
	}

	public Number getRevisionNumber(Object revision) {
		return (Number) revisionIdGetter.get( revision );
	}
}
