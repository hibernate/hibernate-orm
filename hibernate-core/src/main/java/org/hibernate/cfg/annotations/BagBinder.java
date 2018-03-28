/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;

/**
 * Bind a bag.
 *
 * @author Matthew Inger
 */
public class BagBinder extends CollectionBinder {
	public BagBinder() {
		super( false );
	}

	protected Collection createCollection(PersistentClass persistentClass) {
		return new org.hibernate.mapping.Bag( getBuildingContext(), persistentClass );
	}
}
