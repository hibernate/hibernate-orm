package org.hibernate.test.bytecode.enhancement.lazy.group;

import static org.junit.Assert.assertEquals;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

/**
 * Testing OneToOne LazyToOne association
 *
 * @author Jan-Oliver Lustig, Sebastian Viefhaus
 */
public class LazyGroupMappedByTestTask extends AbstractEnhancerTestTask {
    
    public Class<?>[] getAnnotatedClasses() {
		return new Class[] { LGMB_From.class, LGMB_To.class};
	}

	public void prepare() {
		Configuration cfg = new Configuration();
        super.prepare( cfg );		
	}

	public void execute() {
		Long fromId = createEntities();
		
        Statistics stats = getFactory().getStatistics();
        stats.setStatisticsEnabled( true );
        stats.clear();
		

        try (Session session = getFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
            	
            	SessionStatistics sessionStats = session.getStatistics();
                
            	// Should be loaded lazy.
                LGMB_From from = session.get(LGMB_From.class, fromId);                
                assertEquals(1, sessionStats.getEntityCount());
                assertEquals(1, stats.getPrepareStatementCount());
                
                // Lazy text is accessed, toRelation should not be read yet.
                String bigText = from.getBigText();
                assertEquals(1, sessionStats.getEntityCount());
                assertEquals(2, stats.getPrepareStatementCount());

                // Second table is accessed and the lazy one should be reloaded.
                LGMB_To to = from.getToRelation();
                assertEquals(2, sessionStats.getEntityCount());
                assertEquals(3, stats.getPrepareStatementCount());

                to.getFromRelation().getName();
                assertEquals(3, stats.getPrepareStatementCount());
            } finally {
                tx.commit();
            }
        }
		
	}

	protected void cleanup() {
	}
	
	/**
     * Hilfsmethode: Eine Entität anlegen
     * @return ID der Quell-Entität
     */
    public Long createEntities() {
        try (Session session = getFactory().openSession()) {
        	
            Transaction tx = session.beginTransaction();

            session.createSQLQuery("DELETE FROM LGMB_TO").executeUpdate();
            session.createSQLQuery("DELETE FROM LGMB_FROM").executeUpdate();

            LGMB_From from = new LGMB_From("A");
            LGMB_To to = new LGMB_To("B");
            from.setToRelation(to);
            to.setFromRelation(from);

            session.save(from);
            session.flush();

            tx.commit();
            return from.getId();
        }
    }

}
