/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.javassist;

import org.hibernate.bytecode.internal.javassist.BulkAccessor;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
// Extracted from org.hibernate.test.bytecode.ReflectionOptimizerTest.
// I (Yoann) don't know what this tests does, but it's definitely specific to javassist.
public class BulkAccessorTest extends BaseUnitTestCase {

	@Test
	public void testBulkAccessorDirectly() {
		BulkAccessor bulkAccessor = BulkAccessor.create(
				Bean.class,
				BeanReflectionHelper.getGetterNames(),
				BeanReflectionHelper.getSetterNames(),
				BeanReflectionHelper.getTypes()
		);
	}
}
