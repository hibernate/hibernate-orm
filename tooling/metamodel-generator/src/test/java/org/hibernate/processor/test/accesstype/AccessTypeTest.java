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
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertAbsenceOfFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
// TODO - differentiate needed classes per test better. Right now all test classes are processed for each test (HF)
@CompilationTest
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
		User.class,
		Depth1Entity.class,
		Depth2Entity.class,
		Depth3Root.class,
		Depth3Sub.class,
		Depth4Base.class,
		Depth4Entity.class
})
@WithMappingFiles("orm.xml")
class AccessTypeTest {

	@Test
	void testXmlConfiguredEntityGenerated() {
		TestUtil.assertMetamodelClassGeneratedFor( Order.class );
	}

	@Test
	void testExcludeTransientFieldAndStatic() {
		TestUtil.assertAbsenceOfFieldInMetamodelFor( Product.class, "nonPersistent" );
		TestUtil.assertAbsenceOfFieldInMetamodelFor( Product.class, "nonPersistent2" );
	}

	@Test
	void testDefaultAccessTypeOnEntity() {
		TestUtil.assertAbsenceOfFieldInMetamodelFor( User.class, "nonPersistent" );
	}

	@Test
	void testDefaultAccessTypeForSubclassOfEntity() {
		TestUtil.assertAbsenceOfFieldInMetamodelFor( Customer.class, "nonPersistent" );
	}

	@Test
	void testDefaultAccessTypeForEmbeddable() {
		TestUtil.assertAbsenceOfFieldInMetamodelFor( Detail.class, "nonPersistent" );
	}

	@Test
	void testInheritedAccessTypeForEmbeddable() {
		TestUtil.assertAbsenceOfFieldInMetamodelFor( Country.class, "nonPersistent" );
		assertAbsenceOfFieldInMetamodelFor(
				Pet.class, "nonPersistent", "Collection of embeddable not taken care of"
		);
	}

	@Test
	@TestForIssue(jiraKey = " METAGEN-81")
	void testAccessTypeForEmbeddableDeterminedByIdAnnotationInRootEntity() {
		assertPresenceOfFieldInMetamodelFor(
				Hotel.class, "webDomainExpert",
				"Access type should be inherited position of the @Id field annotation in the root entity"
		);
	}

	@Test
	void testDefaultAccessTypeForMappedSuperclass() {
		TestUtil.assertAbsenceOfFieldInMetamodelFor( Detail.class, "volume" );
	}

	@Test
	void testExplicitAccessTypeAndDefaultFromRootEntity() {
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
	void testMemberAccessType() {
		assertPresenceOfFieldInMetamodelFor( Customer.class, "goodPayer", "access type overriding" );
		assertAttributeTypeInMetaModelFor( Customer.class, "goodPayer", Boolean.class, "access type overriding" );
	}

	// test for default access type inference
	@Test
	@TestForIssue(jiraKey = "HHH-13010")
	void depth1IdPlacement() {
		// step 1: field-placed @Id → FIELD access → plain field is a persistent attribute
		assertPresenceOfFieldInMetamodelFor(
				Depth1Entity.class, "probe",
				"field-placed @Id resolves to FIELD access, which includes plain fields"
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13010")
	void depth2CurrentClassMembers() {
		// step 2: the @Id is excluded from id inference by its member-level @Access, so access
		// comes from a mapping annotation on a member of the current class. The @Column is on a
		// getter, giving PROPERTY — never the FIELD the @Id field would yield.
		assertPresenceOfFieldInMetamodelFor(
				Depth2Entity.class, "value",
				"the @Column getter property is a persistent attribute"
		);
		assertAbsenceOfFieldInMetamodelFor(
				Depth2Entity.class, "probe",
				"step 2 resolves to PROPERTY (from the getter), which excludes the plain field"
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13010")
	void depth3RootEntityMembers() {
		// step 3: empty subclass inherits access from the root entity's members.
		// The root's FIELD signal is a @Column field (its @Id is a PROPERTY getter, excluded from inference)
		// so FIELD can only come from a member scan — never from @Id placement.
		assertPresenceOfFieldInMetamodelFor(
				Depth3Sub.class, "probe",
				"access inherited from the root entity's @Column field resolves to FIELD, which includes plain fields"
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13010")
	void depth4MappedSuperclassMembers() {
		// step 4: entity inherits access from the mapped superclass's members. The mapped
		// superclass's FIELD signal is a @Column field (its @Id is a PROPERTY getter, excluded
		// from inference), so FIELD can only come from a member scan — never from @Id placement.
		assertPresenceOfFieldInMetamodelFor(
				Depth4Entity.class, "probe",
				"access inherited from the mapped superclass's @Column field resolves to FIELD, which includes plain fields"
		);
	}
}
