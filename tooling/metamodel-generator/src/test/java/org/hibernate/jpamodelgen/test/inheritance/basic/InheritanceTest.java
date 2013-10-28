/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

