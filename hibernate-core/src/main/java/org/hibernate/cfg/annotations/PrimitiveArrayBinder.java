/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimitiveArray;

/**
 * @author Emmanuel Bernard
 */
public class PrimitiveArrayBinder extends ArrayBinder {
	@Override
	protected Collection createCollection(PersistentClass persistentClass) {
		return new PrimitiveArray( getBuildingContext(), persistentClass );
	}
}
