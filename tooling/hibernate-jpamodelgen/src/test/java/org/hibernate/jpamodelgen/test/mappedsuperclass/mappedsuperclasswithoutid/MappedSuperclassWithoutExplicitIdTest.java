/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.mappedsuperclass.mappedsuperclasswithoutid;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Hardy Ferentschik
 */
public class MappedSuperclassWithoutExplicitIdTest extends CompilationTest {
	@Test
	@WithClasses({ ConcreteProduct.class, Product.class, Shop.class })
	public void testRightAccessTypeForMappedSuperclass() {
		assertMetamodelClassGeneratedFor( ConcreteProduct.class );
		assertMetamodelClassGeneratedFor( Product.class );
		assertMetamodelClassGeneratedFor( Shop.class );
		assertPresenceOfFieldInMetamodelFor( Product.class, "shop", "The many to one attribute shop is missing" );
	}
}
