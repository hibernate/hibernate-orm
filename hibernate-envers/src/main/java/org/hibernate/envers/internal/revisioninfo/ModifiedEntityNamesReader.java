/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.revisioninfo;

import java.util.Set;

import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.property.access.spi.PropertyValueAccessor;
import org.hibernate.service.ServiceRegistry;

/**
 * Returns modified entity names from a persisted revision info entity.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class ModifiedEntityNamesReader {
	private final PropertyValueAccessor modifiedEntityNamesGetter;

	public ModifiedEntityNamesReader(
			Class<?> revisionInfoClass,
			PropertyData modifiedEntityNamesData,
			ServiceRegistry serviceRegistry) {
		modifiedEntityNamesGetter = ReflectionTools.getPropertyValueAccessor( revisionInfoClass, modifiedEntityNamesData, serviceRegistry );
	}

	@SuppressWarnings("unchecked")
	public Set<String> getModifiedEntityNames(Object revisionEntity) {
		return (Set<String>) modifiedEntityNamesGetter.get( revisionEntity );
	}
}
