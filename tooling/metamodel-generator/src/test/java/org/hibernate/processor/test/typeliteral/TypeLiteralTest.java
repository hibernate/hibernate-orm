package org.hibernate.processor.test.typeliteral;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;

import org.junit.Test;

import jakarta.persistence.EntityManager;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfMethodInMetamodelFor;

public class TypeLiteralTest extends CompilationTest {

	@Test
	@WithClasses(
			value = {},
			sources = "org.hibernate.processor.test.typeliteral.Simple"
	)
	@TestForIssue(jiraKey = "HHH-18358")
	public void namedQueryWithTypeLiteral() {
		final String entityClass = "org.hibernate.processor.test.typeliteral.Simple";

		System.out.println( TestUtil.getMetaModelSourceAsString( entityClass ) );

		assertMetamodelClassGeneratedFor( entityClass );

		assertPresenceOfFieldInMetamodelFor( entityClass, "QUERY_SIMPLE" );
		assertPresenceOfFieldInMetamodelFor( entityClass, "QUERY_LONGER" );

		assertPresenceOfMethodInMetamodelFor( entityClass, "simple", EntityManager.class );
		assertPresenceOfMethodInMetamodelFor( entityClass, "longer", EntityManager.class );
	}
}
