/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.internal;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.cfg.SecondPass;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Selectable;

public class NullableDiscriminatorColumnSecondPass implements SecondPass {
	private final String rootEntityName;

	public NullableDiscriminatorColumnSecondPass(String rootEntityName) {
		this.rootEntityName = rootEntityName;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void doSecondPass(Map persistentClasses) throws MappingException {
		PersistentClass rootPersistenceClass = (PersistentClass) persistentClasses.get( rootEntityName );
		if ( hasNullDiscriminatorValue( rootPersistenceClass ) ) {
			for ( Iterator<Selectable> iterator = rootPersistenceClass.getDiscriminator().getColumnIterator();
					iterator.hasNext(); ) {
				Selectable selectable = iterator.next();
				if ( selectable instanceof Column ) {
					( (Column) selectable ).setNullable( true );
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	private boolean hasNullDiscriminatorValue(PersistentClass rootPersistenceClass) {
		if ( rootPersistenceClass.isDiscriminatorValueNull() ) {
			return true;
		}
		Iterator<PersistentClass> subclassIterator = rootPersistenceClass.getSubclassIterator();
		while ( subclassIterator.hasNext() ) {
			if ( subclassIterator.next().isDiscriminatorValueNull() ) {
				return true;
			}
		}
		return false;
	}
}
