/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;

import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;

import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;

/**
 * @author Andrea Boriero
 */
public class NoDirtyCheckEnhancementContext extends EnhancerTestContext {
	@Override
	public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
		return false;
	}

	@Override
	public boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
		return false;
	}

	@Override
	public boolean doBiDirectionalAssociationManagement(UnloadedField field) {
		return false;
	}
}
