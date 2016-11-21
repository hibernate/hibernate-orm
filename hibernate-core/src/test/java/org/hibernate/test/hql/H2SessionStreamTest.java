/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Stream;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
@TestForIssue(jiraKey = "HHH-11260")
public class H2SessionStreamTest extends BaseNonConfigCoreFunctionalTestCase {

	private PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();

	@Override
	protected void addSettings(Map settings) {
		settings.put(
				AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
	}

	@Override
	protected void releaseResources() {
		super.releaseResources();
		connectionProvider.stop();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class };
	}

	@Override
	protected void prepareTest() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			session.save( new MyEntity( 1L, "entity_1" ) );
			session.save( new MyEntity( 2L, "entity_2" ) );
		} );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	public void testStream() {
		doInHibernate( this::sessionFactory, session -> {
			connectionProvider.clear();
			try ( Stream stream = session.createQuery(
					"select me from MyEntity me" ).stream() ) {
				stream.forEach( Assert::assertNotNull );
			}
			assertEquals(
					1,
					connectionProvider.getPreparedStatements().size()
			);
			PreparedStatement preparedStatement = connectionProvider.getPreparedStatements()
					.get( 0 );
			try {
				verify( preparedStatement, never() ).setFetchSize( anyInt() );
			}
			catch ( SQLException e ) {
				fail( e.getMessage() );
			}
		} );
	}

	@Test
	public void testStreamSubQuery() {
		doInHibernate( this::sessionFactory, session -> {
			connectionProvider.clear();
			try ( Stream stream = session.createQuery(
					"select me from MyEntity me" ).stream() ) {
				stream.forEach( Assert::assertNotNull );
				long count = session.createQuery( "select count(*) from MyEntity", Long.class).getSingleResult();
				assertEquals( 2, count );
			}
		} );
	}

	@Entity(name = "MyEntity")
	@Table(name = "MY_ENTITY")
	public static class MyEntity {
		@Id
		private Long id;

		private String description;

		public MyEntity() {
		}

		public MyEntity(Long id, String description) {
			this.id = id;
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

}
