/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.bytecode.enhancement;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;

/**
 * Enhancement context used in tests
 *
 * @author Luis Barreiro
 */
public class EnhancerTestContext extends DefaultEnhancementContext {

	@Override
	public boolean doBiDirectionalAssociationManagement(UnloadedField field) {
		return true;
	}

	@Override
	public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
		return true;
	}

	@Override
	public boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
		return true;
	}

	@Override
	public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
		return true;
	}

	@Override
	public boolean isLazyLoadable(UnloadedField field) {
		return true;
	}

}
