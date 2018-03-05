/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.nationalized;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.Nationalized;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10364")
@SkipForDialect(value = DB2Dialect.class, comment = "DB2 jdbc driver doesn't support getNClob")
@SkipForDialect(value = MySQLDialect.class, comment = "MySQL/MariadB doesn't support nclob")
@SkipForDialect(value = PostgreSQL81Dialect.class, comment = "PostgreSQL doesn't support nclob")
@SkipForDialect(value = SybaseDialect.class, comment = "Sybase doesn't support nclob")
public class NationalizedLobFieldTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {MyEntity.class};
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "false" );
	}

	@Test
	public void testNationalization() {
		Session s = openSession();
		s.getTransaction().begin();
		try {
			MyEntity e = new MyEntity( 1L );
			e.setState( "UK" );
			s.save( e );
			s.getTransaction().commit();
		}
		catch (Exception e) {
			if ( s.getTransaction().getStatus() != TransactionStatus.FAILED_COMMIT ) {
				s.getTransaction().rollback();
			}
			fail( e.getMessage() );
		}
		finally {
			s.close();
		}

		s = openSession();
		try {
			MyEntity myEntity = s.get( MyEntity.class, 1L );
			assertNotNull( myEntity );
			assertThat( myEntity.getState(), is( "UK" ) );
		}
		finally {
			s.close();
		}

	}

	@Entity(name = "MyEntity")
	@Table(name = "my_entity")
	public static class MyEntity {
		@Id
		private long id;

		@Lob
		@Nationalized
		private String state;

		public MyEntity() {
		}

		public MyEntity(long id) {
			this.id = id;
		}

		public long getId() {
			return id;
		}

		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}
	}
}
