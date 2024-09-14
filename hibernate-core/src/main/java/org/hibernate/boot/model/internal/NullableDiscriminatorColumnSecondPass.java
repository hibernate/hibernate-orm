/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
