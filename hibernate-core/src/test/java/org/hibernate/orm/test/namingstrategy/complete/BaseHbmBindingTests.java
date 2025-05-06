/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy.complete;

import org.hibernate.boot.MetadataSources;

/**
 * @author Steve Ebersole
 */
public abstract class BaseHbmBindingTests extends BaseNamingTests {
	@Override
	protected void applySources(MetadataSources metadataSources) {
		metadataSources.addResource( "org/hibernate/orm/test/namingstrategy/complete/Mappings.hbm.xml" );
	}
}
