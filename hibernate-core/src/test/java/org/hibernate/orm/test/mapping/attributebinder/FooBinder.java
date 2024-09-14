/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.attributebinder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.binder.AttributeBinder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

/**
 * The binder to verify binders are called only once.
 *
 * @author Yanming Zhou
 */
public class FooBinder implements AttributeBinder<Foo> {

	private static final Map<String, Foo> map = new ConcurrentHashMap<>();

	@Override
	public void bind(
			Foo annotation,
			MetadataBuildingContext buildingContext,
			PersistentClass persistentClass,
			Property property) {
			String key = persistentClass.getClassName() + "." + property.getName();
			Foo existing = map.putIfAbsent( key, annotation );
			if ( existing == annotation ) {
				throw new IllegalStateException( "AttributeBinder is called twice" );
			}
	}
}
