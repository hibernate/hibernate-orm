package org.hibernate.test.annotations.lob.hhh4635;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.jboss.logging.Logger;
import org.junit.Test;

/**
 * To reproduce this issue, Oracle MUST use a multi-byte character set (UTF-8)!
 * 
 * @author Brett Meyer
 */
@RequiresDialect( Oracle8iDialect.class )
@TestForIssue( jiraKey = "HHH-4635" )
public class LobTest extends BaseCoreFunctionalTestCase {
	
	private static final Logger LOG = Logger.getLogger( LobTest.class );

	@Test
	public void hibernateTest() {
		printConfig();
		
		Session session = openSession();
		session.beginTransaction();
		LobTestEntity entity = new LobTestEntity();
		entity.setId(1L);
		entity.setLobValue(session.getLobHelper().createBlob(new byte[9999]));
		entity.setQwerty(randomString(4000));
		session.save(entity);
		session.getTransaction().commit();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { LobTestEntity.class };
	}
	
	private String randomString( int count ) {
		StringBuilder buffer = new StringBuilder(count);
		for( int i = 0; i < count; i++ ) {
			buffer.append( 'a' );
		}
		return buffer.toString();
	}

	private void printConfig() {
		String sql = "select value from V$NLS_PARAMETERS where parameter = 'NLS_CHARACTERSET'";
		
		Session session = openSession();
		session.beginTransaction();
		Query query = session.createSQLQuery( sql );
		
		String s = (String) query.uniqueResult();
		LOG.debug( "Using Oracle charset " + s );
	}
}
