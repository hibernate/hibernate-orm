/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.spi.delegation;


import org.hibernate.boot.spi.AbstractDelegatingMetadata;
import org.hibernate.boot.spi.MetadataImplementor;

/**
 * If this class does not compile anymore due to unimplemented methods, you should probably add the corresponding
 * methods to the parent class.
 *
 * @author Guillaume Smet
 */
public class TestDelegatingMetadata extends AbstractDelegatingMetadata {

	public TestDelegatingMetadata(MetadataImplementor delegate) {
		super( delegate );
	}
}
