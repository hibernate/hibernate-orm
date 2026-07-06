/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy.complete;

import org.hibernate.boot.pipeline.internal.source.MappingSources;

/**
 * @author Steve Ebersole
 */
public abstract class BaseAnnotationBindingTests extends BaseNamingTests {
	@Override
	protected void applySources(MappingSources mappingSources) {
		mappingSources.addManagedClass( Address.class )
				.addManagedClass( Customer.class )
				.addManagedClass( Industry.class )
				.addManagedClass( Order.class )
				.addManagedClass( ZipCode.class );
	}
}
