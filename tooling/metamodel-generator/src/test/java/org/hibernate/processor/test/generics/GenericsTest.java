/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.generics;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Emmanuel Bernard
 */
public class GenericsTest extends CompilationTest {

	@Test
	@WithClasses({ Parent.class, Child.class })
	public void testGenerics() {
		assertMetamodelClassGeneratedFor( Parent.class );
		assertMetamodelClassGeneratedFor( Child.class );
	}
}
