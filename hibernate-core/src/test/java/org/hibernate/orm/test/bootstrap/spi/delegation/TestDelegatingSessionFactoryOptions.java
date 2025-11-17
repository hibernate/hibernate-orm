/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.spi.delegation;

import org.hibernate.boot.spi.AbstractDelegatingSessionFactoryOptions;
import org.hibernate.boot.spi.SessionFactoryOptions;

/**
 * If this class does not compile anymore due to unimplemented methods, you should probably add the corresponding
 * methods to the parent class.
 *
 * @author Guillaume Smet
 */
public class TestDelegatingSessionFactoryOptions extends AbstractDelegatingSessionFactoryOptions {

	public TestDelegatingSessionFactoryOptions(SessionFactoryOptions delegate) {
		super( delegate );
	}
}
