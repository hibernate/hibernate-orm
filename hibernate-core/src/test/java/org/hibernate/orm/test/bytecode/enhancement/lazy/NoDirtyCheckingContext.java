package org.hibernate.orm.test.bytecode.enhancement.lazy;

import org.hibernate.bytecode.enhance.spi.UnloadedClass;

import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;

public class NoDirtyCheckingContext extends EnhancerTestContext {

	@Override
	public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
		return false;
	}
}
