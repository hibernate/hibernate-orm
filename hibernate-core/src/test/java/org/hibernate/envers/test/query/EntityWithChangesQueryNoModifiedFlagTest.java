/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.query;

import org.hibernate.envers.exception.AuditException;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

/**
 * @author Chris Cranford
 */
@TestForIssue( jiraKey = "HHH-8058" )
public class EntityWithChangesQueryNoModifiedFlagTest extends AbstractEntityWithChangesQueryTest {
	@Override
	@DynamicTest(expected = AuditException.class)
	public void testEntityRevisionsWithChangesQueryNoDeletions() {
		super.testEntityRevisionsWithChangesQueryNoDeletions();
	}

	@Override
	@DynamicTest(expected = AuditException.class)
	public void testEntityRevisionsWithChangesQuery() {
		super.testEntityRevisionsWithChangesQuery();
	}
}
