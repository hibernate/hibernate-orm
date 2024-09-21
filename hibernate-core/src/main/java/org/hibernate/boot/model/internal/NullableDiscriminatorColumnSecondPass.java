/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Subclass;

public class NullableDiscriminatorColumnSecondPass implements SecondPass {
	private final String rootEntityName;

	public NullableDiscriminatorColumnSecondPass(String rootEntityName) {
		this.rootEntityName = rootEntityName;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		final PersistentClass rootPersistenceClass = persistentClasses.get( rootEntityName );
		if ( hasNullDiscriminatorValue( rootPersistenceClass ) ) {
			for ( Selectable selectable: rootPersistenceClass.getDiscriminator().getSelectables() ) {
				if ( selectable instanceof Column ) {
					( (Column) selectable ).setNullable( true );
				}
			}
		}
	}

	private boolean hasNullDiscriminatorValue(PersistentClass rootPersistenceClass) {
		if ( rootPersistenceClass.isDiscriminatorValueNull() ) {
			return true;
		}
		for ( Subclass subclass : rootPersistenceClass.getSubclasses() ) {
			if ( subclass.isDiscriminatorValueNull() ) {
				return true;
			}
		}
		return false;
	}
}
