package org.hibernate.processor.test.constant;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;

import org.junit.Test;

import jakarta.persistence.EntityManager;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfMethodInMetamodelFor;

@TestForIssue(jiraKey = "HHH-18106")
public class ConstantInNamedQueryTest extends CompilationTest {

	@Test
	@WithClasses(value = {}, sources = "org.hibernate.processor.test.constant.CookBookWithCheck")
	public void withCheckHQL() {
		final String entityClass = "org.hibernate.processor.test.constant.CookBookWithCheck";

		System.out.println( TestUtil.getMetaModelSourceAsString( entityClass ) );

		assertMetamodelClassGeneratedFor( entityClass );

		assertPresenceOfFieldInMetamodelFor( entityClass, "QUERY_FIND_BAD_BOOKS" );
		assertPresenceOfFieldInMetamodelFor( entityClass, "QUERY_FIND_GOOD_BOOKS" );

		assertPresenceOfMethodInMetamodelFor( entityClass, "findBadBooks", EntityManager.class );
		assertPresenceOfMethodInMetamodelFor( entityClass, "findGoodBooks", EntityManager.class );
	}

	@Test
	@WithClasses(value = CookBookWithoutCheck.class, sources = "org.hibernate.processor.test.constant.NumericBookType")
	public void withoutCheckHQL() {
		final String entityClass = "org.hibernate.processor.test.constant.CookBookWithoutCheck";

		System.out.println( TestUtil.getMetaModelSourceAsString( entityClass ) );

		assertMetamodelClassGeneratedFor( entityClass );
		assertPresenceOfFieldInMetamodelFor( entityClass, "QUERY_FIND_GOOD_BOOKS" );

		assertPresenceOfMethodInMetamodelFor( entityClass, "findGoodBooks", EntityManager.class );
	}
}
