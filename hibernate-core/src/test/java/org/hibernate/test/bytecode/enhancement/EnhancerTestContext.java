/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement;

import javassist.CtClass;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;

/**
 * Enhancement context used in tests
 *
 * @author Luis Barreiro
 */
public class EnhancerTestContext extends DefaultEnhancementContext {

	@Override
	public boolean doFieldAccessEnhancement(CtClass classDescriptor) {
		return true;
	}
}
