/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.mixedmode;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.hibernate.jpamodelgen.test.util.WithMappingFiles;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertAbsenceOfFieldInMetamodelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Hardy Ferentschik
 */
public class MixedConfigurationTest extends CompilationTest {
	@Test
	@WithClasses({ Car.class, Vehicle.class })
	@WithMappingFiles("car.xml")
	public void testDefaultAccessTypeApplied() {
		assertMetamodelClassGeneratedFor( Vehicle.class );
		assertMetamodelClassGeneratedFor( Car.class );

		assertAbsenceOfFieldInMetamodelFor(
				Car.class, "horsePower", "'horsePower' should not appear in metamodel since it does have no field."
		);
	}

	@Test
	@WithClasses({ Truck.class, Vehicle.class })
	@WithMappingFiles("truck.xml")
	public void testExplicitXmlConfiguredAccessTypeApplied() {
		assertMetamodelClassGeneratedFor( Vehicle.class );
		assertMetamodelClassGeneratedFor( Truck.class );

		assertPresenceOfFieldInMetamodelFor(
				Truck.class, "horsePower", "Property 'horsePower' has explicit access type and should be in metamodel"
		);
		assertAttributeTypeInMetaModelFor( Truck.class, "horsePower", Integer.class, "Wrong meta model type" );
	}

	@Test
	@WithClasses({ Car.class, Vehicle.class, RentalCar.class, RentalCompany.class })
	@WithMappingFiles({ "car.xml", "rentalcar.xml" })
	public void testMixedConfiguration() {
		assertMetamodelClassGeneratedFor( RentalCar.class );
		assertMetamodelClassGeneratedFor( RentalCompany.class );

		assertPresenceOfFieldInMetamodelFor(
				RentalCar.class, "company", "Property 'company' should be included due to xml configuration"
		);
		assertAttributeTypeInMetaModelFor( RentalCar.class, "company", RentalCompany.class, "Wrong meta model type" );

		assertPresenceOfFieldInMetamodelFor(
				RentalCar.class, "insurance", "Property 'insurance' should be included since it is an embeddable"
		);
		assertAttributeTypeInMetaModelFor( RentalCar.class, "insurance", Insurance.class, "Wrong meta model type" );
	}

	@Test
	@WithClasses({ Coordinates.class, ZeroCoordinates.class, Location.class })
	@WithMappingFiles("coordinates.xml")
	public void testAccessTypeForXmlConfiguredEmbeddables() {
		assertMetamodelClassGeneratedFor( Coordinates.class );
		assertPresenceOfFieldInMetamodelFor(
				Coordinates.class, "longitude", "field exists and should be in metamodel"
		);
		assertPresenceOfFieldInMetamodelFor( Coordinates.class, "latitude", "field exists and should be in metamodel" );

		assertMetamodelClassGeneratedFor( ZeroCoordinates.class );
		assertAbsenceOfFieldInMetamodelFor(
				ZeroCoordinates.class,
				"longitude",
				"Field access should be used, but ZeroCoordinates does not define fields"
		);
		assertAbsenceOfFieldInMetamodelFor(
				ZeroCoordinates.class,
				"latitude",
				"Field access should be used, but ZeroCoordinates does not define fields"
		);
	}
}
