/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.generics;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;

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

	@Test
	@WithClasses({NoChildrenGeneric.class})
	public void testGenericsWithoutChildren() {
		assertMetamodelClassGeneratedFor(NoChildrenGeneric.class);
	}

	@Test
	@WithClasses({GenericWithExtendsSuper.class, GenericWithExtendsChild.class})
	public void testGenericsWithExtendDefinition() {
		assertMetamodelClassGeneratedFor(GenericWithExtendsSuper.class);
		assertMetamodelClassGeneratedFor(GenericWithExtendsChild.class);
	}

	@Test
	@WithClasses({TwoChildrenInteger.class, TwoClildrenSuperclass.class, TwoChildrenString.class})
	public void testParentTwoChildren() {
		assertMetamodelClassGeneratedFor(TwoChildrenInteger.class);
		assertMetamodelClassGeneratedFor(TwoClildrenSuperclass.class);
		assertMetamodelClassGeneratedFor(TwoChildrenString.class);
	}
}
