package org.hibernate.test.annotations.lob;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue( jiraKey = "HHH-8103" )
@RequiresDialect( Oracle8iDialect.class )
public class LobWithSequenceIdentityGeneratorTest extends BaseCoreFunctionalTestCase {
	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( Environment.DIALECT, OracleSeqIdGenDialect.class.getName() );
		configuration.setProperty( Environment.USE_NEW_ID_GENERATOR_MAPPINGS, "false" );
		configuration.setProperty( Environment.USE_GET_GENERATED_KEYS, "true" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Document.class };
	}

	@Test
	public void testLobInsertUpdateDeleteSelect() {
		Session session = openSession();

		// Insert
		session.getTransaction().begin();
		Document document = new Document( 1, "HHH-8103", "Oracle expects all LOB properties to be last in INSERT and UPDATE statements." );
		session.persist( document );
		session.getTransaction().commit();

		session.clear();

		session.getTransaction().begin();
		Assert.assertEquals( document, session.get( Document.class, document.getId() ) );
		session.getTransaction().commit();

		session.clear();

		// Update
		session.getTransaction().begin();
		document = (Document) session.get( Document.class, document.getId() );
		document.setFullText( "Correct!" );
		session.update( document );
		session.getTransaction().commit();

		session.clear();

		session.getTransaction().begin();
		Assert.assertEquals( document, session.get( Document.class, document.getId() ) );
		session.getTransaction().commit();

		session.clear();

		// Delete
		session.getTransaction().begin();
		document = (Document) session.get( Document.class, document.getId() );
		session.delete( document );
		session.getTransaction().commit();

		session.clear();

		session.getTransaction().begin();
		Assert.assertNull( session.get( Document.class, document.getId() ) );
		session.getTransaction().commit();

		session.close();
	}
}
