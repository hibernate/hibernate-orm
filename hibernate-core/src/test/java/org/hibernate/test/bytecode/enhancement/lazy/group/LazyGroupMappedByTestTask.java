package org.hibernate.test.bytecode.enhancement.lazy.group;

import static org.junit.Assert.assertEquals;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

/**
 * Testing OneToOne LazyToOne association
 *
 * @author Jan-Oliver Lustig, Sebastian Viefhaus
 */
@TestForIssue(jiraKey = "HHH-11986")
public class LazyGroupMappedByTestTask extends AbstractEnhancerTestTask {
	private Long fromId;

	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { LGMB_From.class, LGMB_To.class };
	}

	@Override
	public void execute() {
		Statistics stats = getFactory().getStatistics();
		stats.setStatisticsEnabled( true );
		stats.clear();

		Session session = getFactory().openSession();
		session.beginTransaction();
		{
					SessionStatistics sessionStats = session.getStatistics();

					// Should be loaded lazy.
					LGMB_From from = session.get( LGMB_From.class, fromId );
					assertEquals( 1, sessionStats.getEntityCount() );
					assertEquals( 1, stats.getPrepareStatementCount() );

					// Lazy text is accessed, toRelation should not be read yet.
					String bigText = from.getBigText();
					assertEquals( 1, sessionStats.getEntityCount() );
					assertEquals( 2, stats.getPrepareStatementCount() );

					// Second table is accessed and the lazy one should be reloaded.
					LGMB_To to = from.getToRelation();
					assertEquals( 2, sessionStats.getEntityCount() );
					assertEquals( 3, stats.getPrepareStatementCount() );

					to.getFromRelation().getName();
					assertEquals( 3, stats.getPrepareStatementCount() );
		}
		session.getTransaction().commit();
		session.close();
	}

	/**
	 * Hilfsmethode: Eine Entität anlegen
	 *
	 * @return ID der Quell-Entität
	 */
	@Override
	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		super.prepare( cfg );

		cleanup();

		Session session = getFactory().openSession();
		session.beginTransaction();
		{
					LGMB_From from = new LGMB_From( "A" );
					LGMB_To to = new LGMB_To( "B" );
					from.setToRelation( to );
					to.setFromRelation( from );

					session.save( from );
					session.flush();

					fromId = from.getId();
		}
		session.getTransaction().commit();
		session.close();
	}

	@Override
	protected void cleanup() {
		Session session = getFactory().openSession();
		session.beginTransaction();
		{
			session.createSQLQuery( "DELETE FROM LGMB_TO" ).executeUpdate();
			session.createSQLQuery( "DELETE FROM LGMB_FROM" ).executeUpdate();

		}
		session.getTransaction().commit();
		session.close();
	}
}
