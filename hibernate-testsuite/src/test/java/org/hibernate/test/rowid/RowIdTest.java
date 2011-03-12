//$Id: RowIdTest.java 11353 2007-03-28 16:03:40Z steve.ebersole@jboss.com $
package org.hibernate.test.rowid;

import java.math.BigDecimal;
import java.sql.Statement;
import java.sql.SQLException;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.testing.junit.functional.DatabaseSpecificFunctionalTestCase;

/**
 * @author Gavin King
 */
public class RowIdTest extends DatabaseSpecificFunctionalTestCase {
	
	public RowIdTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "rowid/Point.hbm.xml" };
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( RowIdTest.class );
	}

	public boolean appliesTo(Dialect dialect) {
		return dialect instanceof Oracle9iDialect;
	}

	public boolean createSchema() {
		return false;
	}

	public void afterSessionFactoryBuilt(SessionFactoryImplementor sfi) {
		super.afterSessionFactoryBuilt( sfi );
		Session session = null;
		try {
			session = sfi.openSession();
			Statement st = session.connection().createStatement();
			try {
				st.execute( "drop table Point");
			}
			catch( Throwable ignore ) {
				// ignore
			}
			st.execute("create table Point (\"x\" number(19,2) not null, \"y\" number(19,2) not null, description varchar2(255) )");
		}
		catch ( SQLException e ) {
			throw new RuntimeException( "Unable to build actual schema : " + e.getMessage() );
		}
		finally {
			if ( session != null ) {
				try {
					session.close();
				}
				catch( Throwable ignore ) {
					// ignore
				}
			}
		}
	}

	public void testRowId() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Point p = new Point( new BigDecimal(1.0), new BigDecimal(1.0) );
		s.persist(p);
		t.commit();
		s.clear();
		
		t = s.beginTransaction();
		p = (Point) s.createCriteria(Point.class).uniqueResult();
		p.setDescription("new desc");
		t.commit();
		s.clear();
		
		t = s.beginTransaction();
		p = (Point) s.createQuery("from Point").uniqueResult();
		p.setDescription("new new desc");
		t.commit();
		s.clear();
		
		t = s.beginTransaction();
		p = (Point) s.get(Point.class, p);
		p.setDescription("new new new desc");
		t.commit();
		s.close();
		
	}

}

