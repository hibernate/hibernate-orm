/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy.complete;

import org.hibernate.boot.MetadataSources;

/**
 * @author Steve Ebersole
 */
public abstract class BaseAnnotationBindingTests extends BaseNamingTests {
	@Override
	protected void applySources(MetadataSources metadataSources) {
		metadataSources.addAnnotatedClass( Address.class )
				.addAnnotatedClass( Customer.class )
				.addAnnotatedClass( Industry.class )
				.addAnnotatedClass( Order.class )
				.addAnnotatedClass( ZipCode.class );
	}
}
