/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi.delegation;

import org.hibernate.boot.spi.AbstractDelegatingMetadataBuilderImplementor;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuilderImplementor;

/**
 * If this class does not compile anymore due to unimplemented methods, you should probably add the corresponding
 * methods to the parent class.
 *
 * @author Guillaume Smet
 */
public class TestDelegatingMetadataBuilderImplementor extends AbstractDelegatingMetadataBuilderImplementor<TestDelegatingMetadataBuilderImplementor> {

	public TestDelegatingMetadataBuilderImplementor(MetadataBuilderImplementor delegate) {
		super( delegate );
	}

	@Override
	protected TestDelegatingMetadataBuilderImplementor getThis() {
		return this;
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		return delegate().getBootstrapContext();
	}
}
