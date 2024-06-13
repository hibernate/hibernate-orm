/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.inheritance.unmappedclassinhierarchy;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertSuperclassRelationshipInMetamodel;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class UnmappedClassInHierarchyTest extends CompilationTest {
	@Test
	@WithClasses({
			BaseEntity.class,
			MappedBase.class,
			NormalExtendsEntity.class,
			NormalExtendsMapped.class,
			SubA.class,
			SubB.class
	})
	public void testUnmappedClassInHierarchy() throws Exception {
		assertSuperclassRelationshipInMetamodel( SubA.class, BaseEntity.class );
		assertSuperclassRelationshipInMetamodel( SubB.class, MappedBase.class );
	}
}
