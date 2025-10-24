/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import org.hibernate.envers.exception.AuditException;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.fail;

/**
 * @author Chris Cranford
 */
@Jpa(annotatedClasses = {
		AbstractEntityWithChangesQueryTest.Simple.class
})
@EnversTest
@JiraKey( value = "HHH-8058" )
public class EntityWithChangesQueryNoModifiedFlagTest extends AbstractEntityWithChangesQueryTest {
	@Test
	public void testEntityRevisionsWithChangesQueryNoDeletions(EntityManagerFactoryScope scope) {
		try {
			super.testEntityRevisionsWithChangesQueryNoDeletions( scope );
			fail( "This should have failed with AuditException since test case doesn't enable modifiedFlag" );
		}
		catch ( Exception e ) {
			assertTyping( AuditException.class, e );
		}
	}

	@Test
	public void testEntityRevisionsWithChangesQuery(EntityManagerFactoryScope scope) {
		try {
			super.testEntityRevisionsWithChangesQuery( scope );
			fail( "This should have failed with AuditException since test case doesn't enable modifiedFlag" );
		}
		catch ( Exception e ) {
			assertTyping( AuditException.class, e );
		}
	}
}
