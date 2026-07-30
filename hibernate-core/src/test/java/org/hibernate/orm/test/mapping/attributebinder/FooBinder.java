/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.attributebinder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.binder.AttributeBinder;
import org.hibernate.binder.AttributeBindingContext;

/**
 * The binder to verify binders are called only once.
 *
 * @author Yanming Zhou
 */
public class FooBinder implements AttributeBinder<Foo> {

	private static final Map<String, Foo> map = new ConcurrentHashMap<>();

	@Override
	public void bind(Foo annotation, AttributeBindingContext context) {
			String key = context.getPersistentClass().getClassName() + "." + context.getProperty().getName();
			Foo existing = map.putIfAbsent( key, annotation );
			if ( existing == annotation ) {
				throw new IllegalStateException( "AttributeBinder is called twice" );
			}
	}
}
