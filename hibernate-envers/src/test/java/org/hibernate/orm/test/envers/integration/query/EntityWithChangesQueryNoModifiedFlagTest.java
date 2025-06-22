/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import org.hibernate.envers.exception.AuditException;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.fail;

/**
 * @author Chris Cranford
 */
@JiraKey( value = "HHH-8058" )
public class EntityWithChangesQueryNoModifiedFlagTest extends AbstractEntityWithChangesQueryTest {
	@Test
	public void testEntityRevisionsWithChangesQueryNoDeletions() {
		try {
			super.testEntityRevisionsWithChangesQueryNoDeletions();
			fail( "This should have failed with AuditException since test case doesn't enable modifiedFlag" );
		}
		catch ( Exception e ) {
			assertTyping( AuditException.class, e );
		}
	}

	@Test
	public void testEntityRevisionsWithChangesQuery() {
		try {
			super.testEntityRevisionsWithChangesQuery();
			fail( "This should have failed with AuditException since test case doesn't enable modifiedFlag" );
		}
		catch ( Exception e ) {
			assertTyping( AuditException.class, e );
		}
	}
}
