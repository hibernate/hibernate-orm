/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.spi.delegation;

import org.hibernate.boot.spi.AbstractDelegatingMetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataBuildingOptions;

/**
 * If this class does not compile anymore due to unimplemented methods, you should probably add the corresponding
 * methods to the parent class.
 *
 * @author Guillaume Smet
 */
public class TestDelegatingMetadataBuildingOptions extends AbstractDelegatingMetadataBuildingOptions {

	public TestDelegatingMetadataBuildingOptions(MetadataBuildingOptions delegate) {
		super( delegate );
	}
}
