package org.hibernate.envers.test.integration.tools;

import java.util.Arrays;

import org.hibernate.Session;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.tools.hbm2ddl.EnversSchemaGenerator;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;

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

	protected boolean createSchema() {
		// Disable schema auto generation.
		return false;
	}

	@Test
	@Priority(10)
	public void testSchemaCreation() {
		// Generate complete schema.
		new EnversSchemaGenerator( metadata() ).export().create( true, true );

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

	@Test
	@Priority(8)
	public void testSchemaDrop() {
		new EnversSchemaGenerator( metadata() ).export().drop( true, true );
	}
}
