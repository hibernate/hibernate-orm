/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.query;

import org.hibernate.envers.exception.AuditException;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.fail;

/**
 * @author Chris Cranford
 */
@TestForIssue( jiraKey = "HHH-8058" )
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
