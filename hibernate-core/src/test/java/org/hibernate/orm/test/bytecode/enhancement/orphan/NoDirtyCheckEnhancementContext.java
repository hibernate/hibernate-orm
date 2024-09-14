/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
