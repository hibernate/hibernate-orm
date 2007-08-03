package org.hibernate.test.perf;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.textui.TestRunner;

import org.hibernate.cfg.Environment;
import org.hibernate.classic.Session;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.ConnectionProviderFactory;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.legacy.Simple;

public class NewerPerformanceTest extends FunctionalTestCase {

	public NewerPerformanceTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] { "legacy/Simple.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( NewerPerformanceTest.class );
	}

	public static void main(String[] args) throws Exception {
		TestRunner.run( suite() );
	}

	public void testMany() throws Exception {

		ConnectionProvider cp = ConnectionProviderFactory.newConnectionProvider( Environment.getProperties() );

		long hiber=0;
		long jdbc=0;

		for ( int n=0; n<20; n++ ) {

			Session s = openSession();
			s.delete("from Simple");
			s.flush();
			Simple[] simples = new Simple[n];
			Serializable[] ids = new Serializable[n];
			for ( int i=0; i<n; i++ ) {
				simples[i] = new Simple();
				simples[i].init();
				simples[i].setCount(i);
				ids[i] = new Long(i);
				s.save(simples[i], ids[i]);
			}
			s.flush();
			s.connection().commit();
			s.close();

			//allow cache to settle

			s = openSession();
			hibernate(s, simples, ids, n, "h0");
			s.close();

			Connection c = cp.getConnection();
			directJDBC( c, simples, ids, n, "j0" );
			cp.closeConnection(c);

			s = openSession();
			hibernate(s, simples, ids, n, "h0");
			s.close();

			c = cp.getConnection();
			directJDBC( c, simples, ids, n, "j0" );
			cp.closeConnection(c);

			//Now do timings

			int N=30;

			long time = System.currentTimeMillis();
			for (int i=0; i<N; i++) {
				s = openSession();
				hibernate(s, simples, ids, n, "h1");
				s.close();
			}
			hiber += System.currentTimeMillis() - time;

			time = System.currentTimeMillis();
			for (int i=0; i<N; i++) {
				c = cp.getConnection();
				directJDBC( c, simples, ids, n, "j1" );
				cp.closeConnection(c);
			}
			jdbc += System.currentTimeMillis() - time;

			time = System.currentTimeMillis();
			for (int i=0; i<N; i++) {
				s = openSession();
				hibernate(s, simples, ids, n, "h2");
				s.close();
			}
			hiber += System.currentTimeMillis() - time;

			time = System.currentTimeMillis();
			for (int i=0; i<N; i++) {
				c = cp.getConnection();
				directJDBC( c, simples, ids, n, "j2" );
				cp.closeConnection(c);
			}
			jdbc += System.currentTimeMillis() - time;

			time = System.currentTimeMillis();
			for (int i=0; i<N; i++) {
				s = openSession();
				hibernate(s, simples, ids, n, "h1");
				s.close();
			}
			hiber += System.currentTimeMillis() - time;

			time = System.currentTimeMillis();
			for (int i=0; i<N; i++) {
				c = cp.getConnection();
				directJDBC( c, simples, ids, n, "j1" );
				cp.closeConnection(c);
			}
			jdbc += System.currentTimeMillis() - time;

		}

		System.out.println( "Hibernate: " + hiber + "ms / Direct JDBC: " + jdbc + "ms = Ratio: " + ( (float) hiber )/jdbc );

		cp.close();
		System.gc();
	}

	public void testSimultaneous() throws Exception {

		ConnectionProvider cp = ConnectionProviderFactory.newConnectionProvider( Environment.getProperties() );

		for ( int n=2; n<4000; n*=2 ) {

			Session s = openSession();
			s.delete("from Simple");
			s.flush();
			Simple[] simples = new Simple[n];
			Serializable[] ids = new Serializable[n];
			for ( int i=0; i<n; i++ ) {
				simples[i] = new Simple();
				simples[i].init();
				simples[i].setCount(i);
				ids[i] = new Long(i);
				s.save(simples[i], ids[i]);
			}
			s.flush();
			s.connection().commit();
			s.close();

			//allow cache to settle

			s = openSession();
			hibernate(s, simples, ids, n, "h0");
			s.close();

			Connection c = cp.getConnection();
			directJDBC( c, simples, ids, n, "j0" );
			cp.closeConnection(c);

			s = openSession();
			hibernate(s, simples, ids, n, "h0");
			s.close();

			c = cp.getConnection();
			directJDBC( c, simples, ids, n, "j0" );
			cp.closeConnection(c);

			//Now do timings

			s = openSession();
			long time = System.currentTimeMillis();
			hibernate(s, simples, ids, n, "h1");
			long hiber = System.currentTimeMillis() - time;
			s.close();

			c = cp.getConnection();
			time = System.currentTimeMillis();
			directJDBC( c, simples, ids, n, "j1" );
			long jdbc = System.currentTimeMillis() - time;
			cp.closeConnection(c);

			s = openSession();
			time = System.currentTimeMillis();
			hibernate(s, simples, ids, n, "h2");
			hiber += System.currentTimeMillis() - time;
			s.close();

			c = cp.getConnection();
			time = System.currentTimeMillis();
			directJDBC( c, simples, ids, n, "j2" );
			jdbc += System.currentTimeMillis() - time;
			cp.closeConnection(c);

			s = openSession();
			time = System.currentTimeMillis();
			hibernate(s, simples, ids, n, "h2");
			hiber += System.currentTimeMillis() - time;
			s.close();

			c = cp.getConnection();
			time = System.currentTimeMillis();
			directJDBC( c, simples, ids, n, "j2" );
			jdbc += System.currentTimeMillis() - time;
			cp.closeConnection(c);

			System.out.println( "Objects: " + n + " - Hibernate: " + hiber + "ms / Direct JDBC: " + jdbc + "ms = Ratio: " + ( (float) hiber )/jdbc );

		}

		cp.close();
		System.gc();
	}

	public void testHibernateOnly() throws Exception {

		for ( int n=2; n<4000; n*=2 ) {

			Session s = openSession();
			Simple[] simples = new Simple[n];
			s.delete("from Simple");
			s.flush();
			Serializable[] ids = new Serializable[n];
			for ( int i=0; i<n; i++ ) {
				simples[i] = new Simple();
				simples[i].init();
				simples[i].setCount(i);
				ids[i] = new Long(i);
				s.save(simples[i], ids[i]);
			}
			s.flush();
			s.connection().commit();
			s.close();

			//Now do timings

			s = openSession();
			long time = System.currentTimeMillis();
			hibernate(s, simples, ids, n, "h1");
			long hiber = System.currentTimeMillis() - time;
			s.close();

			s = openSession();
			time = System.currentTimeMillis();
			hibernate(s, simples, ids, n, "h2");
			hiber += System.currentTimeMillis() - time;
			s.close();

			s = openSession();
			time = System.currentTimeMillis();
			hibernate(s, simples, ids, n, "h2");
			hiber += System.currentTimeMillis() - time;
			s.close();

			System.out.println( "Objects: " + n + " - Hibernate: " + hiber );

		}

		System.gc();
	}

	public void testJdbcOnly() throws Exception {

		ConnectionProvider cp = ConnectionProviderFactory.newConnectionProvider( Environment.getProperties() );

		for ( int n=2; n<4000; n*=2 ) {

			Session s = openSession();
			Simple[] simples = new Simple[n];
			s.delete("from Simple");
			s.flush();
			Serializable[] ids = new Serializable[n];
			for ( int i=0; i<n; i++ ) {
				simples[i] = new Simple();
				simples[i].init();
				simples[i].setCount(i);
				ids[i] = new Long(i);
				s.save(simples[i], ids[i]);
			}
			s.flush();
			s.connection().commit();
			s.close();


			//Now do timings

			Connection c = cp.getConnection();
			long time = System.currentTimeMillis();
			directJDBC( c, simples, ids, n, "j1" );
			long jdbc = System.currentTimeMillis() - time;
			cp.closeConnection(c);

			c = cp.getConnection();
			time = System.currentTimeMillis();
			directJDBC( c, simples, ids, n, "j2" );
			jdbc += System.currentTimeMillis() - time;
			cp.closeConnection(c);

			c = cp.getConnection();
			time = System.currentTimeMillis();
			directJDBC( c, simples, ids, n, "j2" );
			jdbc += System.currentTimeMillis() - time;
			cp.closeConnection(c);

			System.out.println( "Objects: " + n + " Direct JDBC: " + jdbc );

		}

		cp.close();
		System.gc();
	}

	/*private void hibernate(Session s, Simple[] simples, Serializable[] ids, int N, String runname) throws Exception {
		for ( int i=0; i<N; i++ ) {
			s.get( Simple.class, ids[i] );
			//s.find("from Simple s where s.id = ?", ids[i], Hibernate.LONG);
		}
		//s.flush();
		s.connection().commit();
	}

	private void directJDBC(Connection c, Simple[] simples, Serializable[] ids, int N, String runname) throws SQLException {

		for ( int i=0; i<N; i++ ) {
			PreparedStatement select = c.prepareStatement("SELECT s.id_, s.name, s.address, s.count_, s.date_, s.pay, s.other FROM Simple s where s.id_=?");
			select.setLong( 1, ( (Long) ids[i] ).longValue() );
			ResultSet rs = select.executeQuery();
			rs.next();
			/*new Long( rs.getLong(1) );
			rs.getString(2);
			rs.getString(3);
			rs.getInt(4);
			rs.getDate(5);
			rs.getFloat(6);
			rs.getLong(7);*/
			/*Simple s = new Simple();
			new Long( rs.getLong("id_") );  rs.wasNull();
			s.setName( rs.getString("name") );  rs.wasNull();
			s.setAddress( rs.getString("address") );  rs.wasNull();
			s.setCount( rs.getInt("count_") );  rs.wasNull();
			s.setDate( rs.getTimestamp("date_") );  rs.wasNull();
			s.setPay( new Float( rs.getFloat("pay") ) );  rs.wasNull();
			rs.getLong("other"); rs.wasNull(); s.setOther(null);
			rs.close();
			select.close();
		}
		c.commit();
	}*/

	private void hibernate(Session s, Simple[] simples, Serializable[] ids, int N, String runname) throws Exception {
		s.createQuery("from Simple s").list();
		s.connection().commit();
	}

	private void directJDBC(Connection c, Simple[] simples, Serializable[] ids, int N, String runname) throws SQLException {
		List result=new ArrayList();
		PreparedStatement select = c.prepareStatement("SELECT s.id_ as id_, s.name as name, s.address as address, s.count_ as count_, s.date_ as date_, s.pay as pay, s.other as other FROM Simple s");
		ResultSet rs = select.executeQuery();
		while ( rs.next() ) {
			/*new Long( rs.getLong(1) );
			rs.getString(2);
			rs.getString(3);
			rs.getInt(4);
			rs.getDate(5);
			rs.getFloat(6);
			rs.getLong(7);*/
			Simple s = new Simple();
			new Long( rs.getLong("id_") );  rs.wasNull();
			s.setName( rs.getString("name") );  rs.wasNull();
			s.setAddress( rs.getString("address") );  rs.wasNull();
			s.setCount( rs.getInt("count_") );  rs.wasNull();
			s.setDate( rs.getTimestamp("date_") );  rs.wasNull();
			s.setPay( new Float( rs.getFloat("pay") ) );  rs.wasNull();
			rs.getLong("other"); rs.wasNull(); s.setOther(null);
			result.add(s);
		}
		rs.close();
		select.close();
		c.commit();
	}

}
