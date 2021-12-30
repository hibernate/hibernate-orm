/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimitiveArray;
import org.hibernate.mapping.SemanticsResolver;

/**
 * @author Emmanuel Bernard
 */
public class PrimitiveArrayBinder extends ArrayBinder {
	public PrimitiveArrayBinder(SemanticsResolver semanticsResolver, MetadataBuildingContext buildingContext) {
		super( semanticsResolver, buildingContext );
	}

	@Override
	protected Collection createCollection(PersistentClass owner) {
		return new PrimitiveArray( getSemanticsResolver(), owner, getBuildingContext() );
	}
}
