/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.tools;

import java.util.Arrays;

import org.hibernate.Session;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7106")
public class SchemaExportTest extends BaseEnversFunctionalTestCase {
	private Integer id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class};
	}

	@Test
	@Priority(10)
	public void testSchemaCreation() {
		// Populate database with test data.
		Session session = getSession();
		session.getTransaction().begin();
		StrTestEntity entity = new StrTestEntity( "data" );
		session.save( entity );
		session.getTransaction().commit();

		id = entity.getId();
	}

	@Test
	@Priority(9)
	public void testAuditDataRetrieval() {
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( StrTestEntity.class, id ) );
		Assert.assertEquals( new StrTestEntity( "data", id ), getAuditReader().find( StrTestEntity.class, id, 1 ) );
	}
}
