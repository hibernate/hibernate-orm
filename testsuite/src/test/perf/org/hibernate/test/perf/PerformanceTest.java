package org.hibernate.test.perf;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.hibernate.cfg.Environment;
import org.hibernate.classic.Session;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.ConnectionProviderFactory;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.test.legacy.Simple;

public class PerformanceTest extends FunctionalTestCase {

	public PerformanceTest(String arg0) {
		super(arg0);
	}

	public String[] getMappings() {
		return new String[] { "legacy/Simple.hbm.xml" };
	}

	public static Test suite() throws Exception {
		return new TestSuite(PerformanceTest.class);
	}

	public static void main(String[] args) throws Exception {
		TestRunner.run( suite() );
	}

	public void testMany() throws Exception {

		ConnectionProvider cp = ConnectionProviderFactory.newConnectionProvider( Environment.getProperties() );

		long hiber=0;
		long jdbc=0;

		for ( int n=0; n<20; n++ ) {

			Simple[] simples = new Simple[n];
			Serializable[] ids = new Serializable[n];
			for ( int i=0; i<n; i++ ) {
				simples[i] = new Simple();
				simples[i].init();
				simples[i].setCount(i);
				ids[i] = new Long(i);
			}

			//allow cache to settle

			Session s = openSession();
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

			Simple[] simples = new Simple[n];
			Serializable[] ids = new Serializable[n];
			for ( int i=0; i<n; i++ ) {
				simples[i] = new Simple();
				simples[i].init();
				simples[i].setCount(i);
				ids[i] = new Long(i);
			}

			//allow cache to settle

			Session s = openSession();
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

			Simple[] simples = new Simple[n];
			Serializable[] ids = new Serializable[n];
			for ( int i=0; i<n; i++ ) {
				simples[i] = new Simple();
				simples[i].init();
				simples[i].setCount(i);
				ids[i] = new Long(i);
			}

			//Now do timings

			Session s = openSession();
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

			Simple[] simples = new Simple[n];
			Serializable[] ids = new Serializable[n];
			for ( int i=0; i<n; i++ ) {
				simples[i] = new Simple();
				simples[i].init();
				simples[i].setCount(i);
				ids[i] = new Long(i);
			}

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

	private void hibernate(Session s, Simple[] simples, Serializable[] ids, int N, String runname) throws Exception {
		for ( int i=0; i<N; i++ ) {
			s.save( simples[i], ids[i] );
		}
		for ( int i=0; i<N; i++ ) {
			simples[0].setName("A Different Name!" + i + N + runname);
		}
		//s.flush();
		// the results of this test are highly dependent upon
		// how many times we flush!
		assertTrue( "assertion", s.delete("from Simple s")==N );
		s.flush();
		s.connection().commit();
	}

	private void directJDBC(Connection c, Simple[] simples, Serializable[] ids, int N, String runname) throws SQLException {

		PreparedStatement insert = c.prepareStatement("insert into Simple ( name, address, count_, date_, other, id_ ) values ( ?, ?, ?, ?, ?, ? )");
		PreparedStatement delete = c.prepareStatement("delete from Simple where id_ = ?");
		PreparedStatement select = c.prepareStatement("SELECT s.id_, s.name, s.address, s.count_, s.date_, s.other FROM Simple s");
		PreparedStatement update = c.prepareStatement("update Simple set name = ?, address = ?, count_ = ?, date_ = ?, other = ? where id_ = ?");
		for ( int i=0; i<N; i++ ) {
			insert.setString(1, simples[i].getName() );
			insert.setString(2, simples[i].getAddress() );
			insert.setInt(3, simples[i].getCount() );
			insert.setDate( 4, (java.sql.Date) simples[i].getDate() );
			insert.setNull(5, Types.BIGINT);
			insert.setLong( 6, ( (Long) ids[i] ).longValue() );
			insert.executeUpdate();
		}
		insert.close();
		for ( int i=0; i<N; i++ ) {
			update.setString(1, "A Different Name!" + i + N + runname );
			update.setString(2, simples[i].getAddress() );
			update.setInt(3, simples[i].getCount() );
			update.setDate( 4, (java.sql.Date) simples[i].getDate() );
			update.setNull(5, Types.BIGINT);
			update.setLong( 6, ( (Long) ids[i] ).longValue() );
			update.executeUpdate();
		}
		update.close();
		java.sql.ResultSet rs = select.executeQuery();
		Long[] keys = new Long[N];
		int j=0;
		while ( rs.next() ) {
			keys[j++] = new Long( rs.getLong(1) );
			rs.getString(2);
			rs.getString(3);
			rs.getInt(4);
			rs.getDate(5);
			rs.getLong(6);
		}
		rs.close();
		select.close();
		for ( int i=0; i<N; i++ ) {
			delete.setLong(1, keys[i].longValue() );
			delete.executeUpdate();
		}
		delete.close();
		c.commit();
	}
}
