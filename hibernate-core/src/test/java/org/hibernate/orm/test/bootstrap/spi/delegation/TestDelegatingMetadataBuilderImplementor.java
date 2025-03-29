/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.spi.delegation;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.spi.AbstractDelegatingMetadataBuilderImplementor;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.metamodel.CollectionClassification;

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

	@Override
	public MetadataBuilder applyImplicitListSemantics(CollectionClassification classification) {
		return delegate().applyImplicitListSemantics( classification );
	}
}
