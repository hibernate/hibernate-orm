/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.delegation;

import org.hibernate.SessionBuilder;
import org.hibernate.engine.spi.AbstractDelegatingSessionBuilder;

/**
 * If this class does not compile anymore due to unimplemented methods, you should probably add the corresponding
 * methods to the parent class.
 *
 * NOTE: Do not remove!!! Used to verify that delegating SessionBuilder impls compile (aka, validates binary
 * compatibility against previous versions)
 *
 * @author Guillaume Smet
 */
@SuppressWarnings("unused")
public class TestDelegatingSessionBuilder extends AbstractDelegatingSessionBuilder {

	public TestDelegatingSessionBuilder(SessionBuilder delegate) {
		super( delegate );
	}

	@Override
	protected TestDelegatingSessionBuilder getThis() {
		return this;
	}
}
