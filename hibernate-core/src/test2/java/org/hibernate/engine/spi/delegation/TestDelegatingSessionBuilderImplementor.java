/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi.delegation;

import org.hibernate.engine.spi.AbstractDelegatingSessionBuilderImplementor;
import org.hibernate.engine.spi.SessionBuilderImplementor;

/**
 * If this class does not compile anymore due to unimplemented methods, you should probably add the corresponding
 * methods to the parent class.
 *
 * @author Guillaume Smet
 */
public class TestDelegatingSessionBuilderImplementor extends AbstractDelegatingSessionBuilderImplementor<TestDelegatingSessionBuilderImplementor> {

	public TestDelegatingSessionBuilderImplementor(SessionBuilderImplementor<TestDelegatingSessionBuilderImplementor> delegate) {
		super( delegate );
	}

	@Override
	protected TestDelegatingSessionBuilderImplementor getThis() {
		return this;
	}
}
