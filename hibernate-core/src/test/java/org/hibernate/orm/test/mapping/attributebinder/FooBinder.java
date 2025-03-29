/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
