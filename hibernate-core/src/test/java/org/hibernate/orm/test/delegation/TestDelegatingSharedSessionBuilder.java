/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.delegation;

import org.hibernate.SharedSessionBuilder;
import org.hibernate.engine.spi.AbstractDelegatingSharedSessionBuilder;


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
public class TestDelegatingSharedSessionBuilder extends AbstractDelegatingSharedSessionBuilder {

	public TestDelegatingSharedSessionBuilder(SharedSessionBuilder delegate) {
		super( delegate );
	}

	@Override
	protected TestDelegatingSharedSessionBuilder getThis() {
		return this;
	}
}
