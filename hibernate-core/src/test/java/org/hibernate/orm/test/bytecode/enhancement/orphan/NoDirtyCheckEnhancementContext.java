/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.orphan;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;

public class NoDirtyCheckEnhancementContext extends DefaultEnhancementContext {
	@Override
	public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
		return true;
	}

	@Override
	public boolean isLazyLoadable(UnloadedField field) {
		return true;
	}

	@Override
	public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
		return false;
	}
}
