package org.hibernate.processor.test.packageinfo;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;

import org.junit.Test;

import jakarta.persistence.EntityManager;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfMethodInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

public class PackageInfoMetamodelTest extends CompilationTest {

	@Test
	@WithClasses(value = {}, sources = {
			"org.hibernate.processor.test.packageinfo.Message",
			"org.hibernate.processor.test.packageinfo.package-info"
	})
	public void test() {
		assertMetamodelClassGeneratedFor( "org.hibernate.processor.test.packageinfo.Message" );

		System.out.println( getMetaModelSourceAsString( "org.hibernate.processor.test.packageinfo.packageinfo" ) );

		assertPresenceOfFieldInMetamodelFor(
				"org.hibernate.processor.test.packageinfo.packageinfo",
				"QUERY_FIND_BY_KEY"
		);
		assertPresenceOfFieldInMetamodelFor(
				"org.hibernate.processor.test.packageinfo.packageinfo",
				"QUERY_FIND_BY_ID_AND_KEY"
		);

		assertPresenceOfMethodInMetamodelFor(
				"org.hibernate.processor.test.packageinfo.packageinfo",
				"findByKey",
				EntityManager.class,
				String.class
		);
	}
}
