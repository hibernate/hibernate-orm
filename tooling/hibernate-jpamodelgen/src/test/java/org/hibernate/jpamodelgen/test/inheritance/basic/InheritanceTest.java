/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.inheritance.basic;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertSuperClassRelationShipInMetamodel;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class InheritanceTest extends CompilationTest {
	@Test
	@WithClasses({
			AbstractEntity.class,
			Area.class,
			Building.class,
			Customer.class,
			House.class,
			Person.class,
			User.class
	})
	public void testInheritance() throws Exception {

		// entity inheritance
		assertSuperClassRelationShipInMetamodel( Customer.class, User.class );


		// mapped super class
		assertSuperClassRelationShipInMetamodel( House.class, Building.class );
		assertSuperClassRelationShipInMetamodel( Building.class, Area.class );

		// METAGEN-29
		assertSuperClassRelationShipInMetamodel( Person.class, AbstractEntity.class );
		assertPresenceOfFieldInMetamodelFor( AbstractEntity.class, "id", "Property 'id' should exist" );
		assertPresenceOfFieldInMetamodelFor( AbstractEntity.class, "foo", "Property should exist - METAGEN-29" );
		assertAttributeTypeInMetaModelFor(
				AbstractEntity.class,
				"foo",
				Object.class,
				"Object is the upper bound of foo "
		);

		assertPresenceOfFieldInMetamodelFor( Person.class, "name", "Property 'name' should exist" );
	}
}

