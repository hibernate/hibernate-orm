/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.typeliteral;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;

import org.junit.Test;

import jakarta.persistence.EntityManager;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfMethodInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

public class TypeLiteralTest extends CompilationTest {

	@Test
	@WithClasses(value = {},
			sources = {
					"org.hibernate.processor.test.typeliteral.Account",
					"org.hibernate.processor.test.typeliteral.CreditAccount",
					"org.hibernate.processor.test.typeliteral.DebitAccount"
			})
	@TestForIssue(jiraKey = "HHH-18358")
	public void inheritance() {
		final var entityClass = "org.hibernate.processor.test.typeliteral.Account";
		System.out.println( getMetaModelSourceAsString( entityClass ) );

		assertMetamodelClassGeneratedFor( entityClass );

		assertPresenceOfFieldInMetamodelFor( entityClass, "QUERY_DEBIT_ACCOUNTS" );
		assertPresenceOfFieldInMetamodelFor( entityClass, "QUERY_CREDIT_ACCOUNTS" );

		assertPresenceOfMethodInMetamodelFor( entityClass, "debitAccounts", EntityManager.class );
		assertPresenceOfMethodInMetamodelFor( entityClass, "creditAccounts", EntityManager.class );
	}
}
