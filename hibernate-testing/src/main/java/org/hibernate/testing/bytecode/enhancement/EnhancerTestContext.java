/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.bytecode.enhancement;

import javassist.CtClass;
import javassist.CtField;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;

/**
 * Enhancement context used in tests
 *
 * @author Luis Barreiro
 */
public class EnhancerTestContext extends DefaultEnhancementContext {

	@Override
	public boolean doBiDirectionalAssociationManagement(CtField field) {
		return true;
	}

	@Override
	public boolean doDirtyCheckingInline(CtClass classDescriptor) {
		return true;
	}

	@Override
	public boolean doExtendedEnhancement(CtClass classDescriptor) {
		return true;
	}

	@Override
	public boolean hasLazyLoadableAttributes(CtClass classDescriptor) {
		return true;
	}

	@Override
	public boolean isLazyLoadable(CtField field) {
		return true;
	}

}
