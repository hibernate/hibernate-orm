/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.jpamodelgen.test.mixedmode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.testng.annotations.Test;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestUtil;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertAbsenceOfFieldInMetamodelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Hardy Ferentschik
 */
public class MixedConfigurationTest extends CompilationTest {
	@Test
	public void testDefaultAccessTypeApplied() {
		assertMetamodelClassGeneratedFor( Vehicle.class );
		assertMetamodelClassGeneratedFor( Car.class );

		assertAbsenceOfFieldInMetamodelFor(
				Car.class, "horsePower", "'horsePower' should not appear in metamodel since it does have no field."
		);
	}

	@Test
	public void testExplicitXmlConfiguredAccessTypeApplied() {
		assertMetamodelClassGeneratedFor( Vehicle.class );
		assertMetamodelClassGeneratedFor( Truck.class );

		assertPresenceOfFieldInMetamodelFor(
				Truck.class, "horsePower", "Property 'horsePower' has explicit access type and should be in metamodel"
		);
		assertAttributeTypeInMetaModelFor( Truck.class, "horsePower", Integer.class, "Wrong meta model type" );
	}

	@Test
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

	@Override
	protected String getPackageNameOfCurrentTest() {
		return MixedConfigurationTest.class.getPackage().getName();
	}

	@Override
	protected Collection<String> getOrmFiles() {
		List<String> ormFiles = new ArrayList<String>();
		String dir = TestUtil.fcnToPath( MixedConfigurationTest.class.getPackage().getName() );
		ormFiles.add( dir + "/car.xml" );
		ormFiles.add( dir + "/rentalcar.xml" );
		ormFiles.add( dir + "/truck.xml" );
		ormFiles.add( dir + "/coordinates.xml" );
		return ormFiles;
	}
}
