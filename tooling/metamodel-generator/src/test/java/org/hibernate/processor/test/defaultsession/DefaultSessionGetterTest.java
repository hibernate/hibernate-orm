/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.defaultsession;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class DefaultSessionGetterTest {
	@Test
	@WithClasses({ Book.class, BookDao.class })
	void testDefaultSessionGetterIsRespected() {
		final String dao = getMetaModelSourceAsString( BookDao.class, true );
		System.out.println( dao );
		assertMetamodelClassGeneratedFor( BookDao.class, true );
		// the user's default method provides the session: we don't generate an injection point or override it
		assertFalse( dao.contains( "public _BookDao(" ), dao );
		assertFalse( dao.contains( "public EntityManager entityManager()" ), dao );
		// ... and it is the one used by the generated query method
		assertTrue( dao.contains( "entityManager()" ), dao );
	}
}
