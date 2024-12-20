/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;

/**
 * @author Emmanuel Bernard
 */
public class JoinedSubclassFkSecondPass extends FkSecondPass {
	private final JoinedSubclass entity;
	private final MetadataBuildingContext buildingContext;

	public JoinedSubclassFkSecondPass(
			JoinedSubclass entity,
			AnnotatedJoinColumns inheritanceJoinedColumns,
			SimpleValue key,
			MetadataBuildingContext buildingContext) {
		super( key, inheritanceJoinedColumns );
		this.entity = entity;
		this.buildingContext = buildingContext;
	}

	@Override
	public String getReferencedEntityName() {
		return entity.getSuperclass().getEntityName();
	}

	@Override
	public boolean isInPrimaryKey() {
		return true;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		TableBinder.bindForeignKey( entity.getSuperclass(), entity, columns, value, false, buildingContext );
	}
}
