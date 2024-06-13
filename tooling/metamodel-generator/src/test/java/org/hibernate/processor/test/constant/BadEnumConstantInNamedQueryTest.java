package org.hibernate.processor.test.constant;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;

import org.junit.Test;

import jakarta.persistence.EntityManager;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfMethodInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestForIssue(jiraKey = "HHH-No-Such-Key")
public class BadEnumConstantInNamedQueryTest extends CompilationTest {

	@Test
	@WithClasses({ CookBook.class })
	public void testFourthWithoutCheckHQL() {
		System.out.println( getMetaModelSourceAsString( CookBook.class ) );
		assertMetamodelClassGeneratedFor( CookBook.class );
		assertPresenceOfFieldInMetamodelFor( CookBook.class, "QUERY_FIND_GOOD_BOOKS" );
		assertThrows(
				AssertionError.class,
				() -> assertPresenceOfMethodInMetamodelFor( CookBook.class, "findGoodBooks", EntityManager.class )
		);
	}
}
