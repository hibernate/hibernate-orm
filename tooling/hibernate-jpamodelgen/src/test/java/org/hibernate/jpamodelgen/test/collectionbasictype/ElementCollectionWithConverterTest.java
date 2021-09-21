/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.collectionbasictype;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertListAttributeTypeInMetaModelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMapAttributesInMetaModelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertSetAttributeTypeInMetaModelFor;

/**
 * @author Chris Cranford
 */
public class ElementCollectionWithConverterTest extends CompilationTest {
	@Test
	@TestForIssue(jiraKey = "HHH-12581")
	@WithClasses( { Item.class } )
	public void testConverterAppliedToElementCollections() {
		assertMetamodelClassGeneratedFor( Item.class );

		// Verify that field roles is a SetAttribute with a generic type of Role.class
		assertSetAttributeTypeInMetaModelFor(
				Item.class,
				"roles",
				Role.class,
				"Generic types or attribute class implementation incorrect for property roles"
		);

		// Verify that field providers is a ListAttribute with a generic type of String.class
		assertListAttributeTypeInMetaModelFor(
				Item.class,
				"providers",
				String.class,
				"Generic types or attribute class implementation incorrect for property providers"
		);

		// Verify that field attributes is a MapAttribute with a generic type of
		// String.class for the value and Integer.class for the key
		assertMapAttributesInMetaModelFor(
				Item.class,
				"attributes",
				Integer.class,
				String.class,
				"Generic types or attribute class implementation incorrect for property attributes"
		);
	}
}
