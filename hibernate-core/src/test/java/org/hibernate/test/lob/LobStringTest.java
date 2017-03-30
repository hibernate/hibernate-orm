/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lob;

import java.sql.Clob;
import java.sql.SQLException;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.ClobProxy;
import org.hibernate.query.Query;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11477")
@SkipForDialect(Oracle8iDialect.class)
public class LobStringTest extends BaseCoreFunctionalTestCase {
	//SQL Server - VARCHAR(MAX) is limited to 8000 bytes only
	private static final int LONG_STRING_SIZE = 3999;

	private final String value1 = buildRecursively( LONG_STRING_SIZE, 'x' );
	private final String value2 = buildRecursively( LONG_STRING_SIZE, 'y' );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {TestEntity.class};
	}

	@Override
	protected void prepareTest() throws Exception {
		TestEntity entity = new TestEntity();
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {

			entity.setFirstLobField( value1 );
			entity.setSecondLobField( value2 );
			entity.setClobField( session.getLobHelper().createClob( value2 ) );
			session.save( entity );
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			final TestEntity testEntity = session.find( TestEntity.class, entity.getId() );
			assertThat( testEntity.getFirstLobField(), is( value1 ) );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11477")
	public void testHqlQuery() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			final Query query = session.createQuery( "from TestEntity" );

			final List<TestEntity> results = query.list();

			assertThat( results.size(), is( 1 ) );

			final TestEntity testEntity = results.get( 0 );
			assertThat( testEntity.getFirstLobField(), is( value1 ) );
			assertThat( testEntity.getSecondLobField(), is( value2 ) );
			final Clob clobField = testEntity.getClobField();
			try {

				assertThat( clobField.getSubString( 1, (int) clobField.length() ), is( value2 ) );
			}
			catch (SQLException e) {
				fail( e.getMessage() );
			}
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11477")
	public void testUsingStringLobAnnotatedPropertyInHqlQuery() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			final Query query = session.createQuery( "from TestEntity where LOWER(firstLobField) LIKE :value" );
			query.setParameter( "value", value1 );

			final List<TestEntity> results = query.list();

			assertThat( results.size(), is( 1 ) );

			final TestEntity testEntity = results.get( 0 );
			assertThat( testEntity.getFirstLobField(), is( value1 ) );
			assertThat( testEntity.getSecondLobField(), is( value2 ) );
			final Clob clobField = testEntity.getClobField();
			try {
				assertThat( clobField.getSubString( 1, (int) clobField.length() ), is( value2 ) );
			}
			catch (SQLException e) {
				fail( e.getMessage() );
			}
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11477")
	public void testSelectStringLobAnnotatedInHqlQuery() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			final Query query = session.createQuery(
					"select t.secondLobField from TestEntity t where LOWER(t.firstLobField) LIKE :value" );
			query.setParameter( "value", value1 );
			final List<String> results = query.list();

			assertThat( results.size(), is( 1 ) );

			assertThat( results.get( 0 ), is( value2 ) );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11477")
	public void testUsingLobPropertyInHqlQuery() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			final Query query = session.createQuery(
					"select t.secondLobField from TestEntity t where LOWER(t.clobField) LIKE :value" );
			query.setParameter( "value", ClobProxy.generateProxy( value2 ) );
			final List<String> results = query.list();

			assertThat( results.size(), is( 1 ) );

			assertThat( results.get( 0 ), is( value2 ) );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11477")
	public void testSelectClobPropertyInHqlQuery() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			final Query query = session.createQuery(
					"select t.clobField from TestEntity t where LOWER(t.clobField) LIKE :value" );
			query.setParameter( "value", ClobProxy.generateProxy( value2 ) );
			final List<Clob> results = query.list();

			assertThat( results.size(), is( 1 ) );

			final Clob clobField = results.get( 0 );
			try {
				assertThat( clobField.getSubString( 1, (int) clobField.length() ), is( value2 ) );
			}
			catch (SQLException e) {
				fail( e.getMessage() );
			}
		} );
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		@Id
		@GeneratedValue
		private long id;

		@Lob
		@Column(length = LONG_STRING_SIZE) //needed by HSQLDialect
		String firstLobField;

		@Lob
		@Column(length = LONG_STRING_SIZE) //needed by HSQLDialect
		String secondLobField;

		@Lob
		@Column(length = LONG_STRING_SIZE) //needed by HSQLDialect
		Clob clobField;

		public long getId() {
			return id;
		}

		public String getFirstLobField() {
			return firstLobField;
		}

		public void setFirstLobField(String firstLobField) {
			this.firstLobField = firstLobField;
		}

		public String getSecondLobField() {
			return secondLobField;
		}

		public void setSecondLobField(String secondLobField) {
			this.secondLobField = secondLobField;
		}

		public Clob getClobField() {
			return clobField;
		}

		public void setClobField(Clob clobField) {
			this.clobField = clobField;
		}
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	private String buildRecursively(int size, char baseChar) {
		StringBuilder buff = new StringBuilder();
		for ( int i = 0; i < size; i++ ) {
			buff.append( baseChar );
		}
		return buff.toString();
	}
}
