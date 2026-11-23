package org.hibernate.orm.test.bytecode.enhancement.orphan;

import org.hibernate.testing.bytecode.enhancement.EnhancementTestConfiguration;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;

public class NoDirtyCheckEnhancementContext extends EnhancementTestConfiguration {
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
