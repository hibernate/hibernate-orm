/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.namedquery;

import jakarta.persistence.EntityManager;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfMethodInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfNameFieldInMetamodelFor;

/**
 * @author Gavin King
 */
public class AuxiliaryTest extends CompilationTest {
	@Test
	@WithClasses({ Book.class, Main.class })
	public void test() {
		System.out.println( TestUtil.getMetaModelSourceAsString( Main.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( Book.class ) );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( Main.class );
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"QUERY_BOOK_BY_TITLE",
				"Missing named query attribute."
		);
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"QUERY_BOOK_BY_ISBN",
				"Missing named query attribute."
		);
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"QUERY_BOOK_NATIVE_QUERY",
				"Missing named native query attribute."
		);
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"MAPPING_BOOK_NATIVE_QUERY_RESULT",
				"Missing result set mapping."
		);
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"PROFILE_FETCH_ONE",
				"Missing fetch profile attribute."
		);
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"PROFILE_FETCH_TWO",
				"Missing fetch profile attribute."
		);
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"PROFILE_DUMMY_FETCH",
				"Missing fetch profile attribute."
		);
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"MAPPING_RESULT_SET_MAPPING_ONE",
				"Missing fetch profile attribute."
		);
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"MAPPING_RESULT_SET_MAPPING_TWO",
				"Missing fetch profile attribute."
		);
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"QUERY__SYSDATE_",
				"Missing fetch profile attribute."
		);
		assertPresenceOfMethodInMetamodelFor(
				Main.class,
				"bookByIsbn",
				EntityManager.class,
				String.class
		);
		assertPresenceOfMethodInMetamodelFor(
				Main.class,
				"bookByTitle",
				EntityManager.class,
				String.class
		);

		assertPresenceOfNameFieldInMetamodelFor(
				Book.class,
				"GRAPH_ENTITY_GRAPH",
				"Missing fetch profile attribute."
		);
		assertPresenceOfMethodInMetamodelFor(
				Book.class,
				"findByTitle",
				EntityManager.class,
				String.class
		);
		assertPresenceOfMethodInMetamodelFor(
				Book.class,
				"findByTitleAndType",
				EntityManager.class,
				String.class,
				Type.class
		);
		assertPresenceOfMethodInMetamodelFor(
				Book.class,
				"getTitles",
				EntityManager.class
		);
		assertPresenceOfMethodInMetamodelFor(
				Book.class,
				"getUpperLowerTitles",
				EntityManager.class
		);
		assertPresenceOfMethodInMetamodelFor(
				Book.class,
				"typeOfBook",
				EntityManager.class,
				String.class
		);
		assertPresenceOfMethodInMetamodelFor(
				Book.class,
				"crazy",
				EntityManager.class,
				Object.class,
				Object.class
		);
	}
}
