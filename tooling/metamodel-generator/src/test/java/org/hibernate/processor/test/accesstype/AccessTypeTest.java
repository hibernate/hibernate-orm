/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.accesstype;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithMappingFiles;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertAbsenceOfFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
// TODO - differentiate needed classes per test better. Right now all test classes are processed for each test (HF)
@WithClasses({
		Address.class,
		Area.class,
		Building.class,
		Country.class,
		Customer.class,
		Detail.class,
		Hominidae.class,
		Hotel.class,
		HotelRoom.class,
		House.class,
		Human.class,
		Inhabitant.class,
		Item.class,
		LivingBeing.class,
		Mammals.class,
		Order.class,
		Pet.class,
		Product.class,
		Room.class,
		Shop.class,
		User.class
})
@WithMappingFiles("orm.xml")
public class AccessTypeTest extends CompilationTest {

	@Test
	public void testXmlConfiguredEntityGenerated() {
		TestUtil.assertMetamodelClassGeneratedFor( Order.class );
	}

	@Test
	public void testExcludeTransientFieldAndStatic() {
		TestUtil.assertAbsenceOfFieldInMetamodelFor( Product.class, "nonPersistent" );
		TestUtil.assertAbsenceOfFieldInMetamodelFor( Product.class, "nonPersistent2" );
	}

	@Test
	public void testDefaultAccessTypeOnEntity() {
		TestUtil.assertAbsenceOfFieldInMetamodelFor( User.class, "nonPersistent" );
	}

	@Test
	public void testDefaultAccessTypeForSubclassOfEntity() {
		TestUtil.assertAbsenceOfFieldInMetamodelFor( Customer.class, "nonPersistent" );
	}

	@Test
	public void testDefaultAccessTypeForEmbeddable() {
		TestUtil.assertAbsenceOfFieldInMetamodelFor( Detail.class, "nonPersistent" );
	}

	@Test
	public void testInheritedAccessTypeForEmbeddable() {
		TestUtil.assertAbsenceOfFieldInMetamodelFor( Country.class, "nonPersistent" );
		assertAbsenceOfFieldInMetamodelFor(
				Pet.class, "nonPersistent", "Collection of embeddable not taken care of"
		);
	}

	@Test
	@TestForIssue(jiraKey = " METAGEN-81")
	public void testAccessTypeForEmbeddableDeterminedByIdAnnotationInRootEntity() {
		assertPresenceOfFieldInMetamodelFor(
				Hotel.class, "webDomainExpert",
				"Access type should be inherited position of the @Id field annotation in the root entity"
		);
	}

	@Test
	public void testDefaultAccessTypeForMappedSuperclass() {
		TestUtil.assertAbsenceOfFieldInMetamodelFor( Detail.class, "volume" );
	}

	@Test
	public void testExplicitAccessTypeAndDefaultFromRootEntity() {
		assertAbsenceOfFieldInMetamodelFor(
				LivingBeing.class,
				"nonPersistent",
				"explicit access type on mapped superclass"
		);
		assertAbsenceOfFieldInMetamodelFor( Hominidae.class, "nonPersistent", "explicit access type on entity" );
		assertAbsenceOfFieldInMetamodelFor(
				Human.class,
				"nonPersistent",
				"proper inheritance from root entity access type"
		);
	}

	@Test
	public void testMemberAccessType() {
		assertPresenceOfFieldInMetamodelFor( Customer.class, "goodPayer", "access type overriding" );
		assertAttributeTypeInMetaModelFor( Customer.class, "goodPayer", Boolean.class, "access type overriding" );
	}
}
