/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.spi.delegation;

import org.hibernate.boot.spi.AbstractDelegatingSessionFactoryBuilderImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;


/**
 * If this class does not compile anymore due to unimplemented methods, you should probably add the corresponding
 * methods to the parent class.
 *
 * @author Guillaume Smet
 */
public class TestDelegatingSessionFactoryBuilderImplementor extends AbstractDelegatingSessionFactoryBuilderImplementor<TestDelegatingSessionFactoryBuilderImplementor> {

	public TestDelegatingSessionFactoryBuilderImplementor(SessionFactoryBuilderImplementor delegate) {
		super( delegate );
	}

	@Override
	protected TestDelegatingSessionFactoryBuilderImplementor getThis() {
		return this;
	}
}
