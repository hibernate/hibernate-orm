/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.version;

import java.time.LocalDateTime;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.Session;
import org.hibernate.dialect.MySQL57Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-10026" )
public class LocalDateTimeVersionTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { TheEntity.class };
	}

	@Test
	@RequiresDialect( MySQL57Dialect.class )
	public void testInstantUsageAsVersion() {
		Session session = openSession();
		session.getTransaction().begin();
		TheEntity e = new TheEntity( 1 );
		e.setName( "original name" );
		session.save( e );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		e = session.byId( TheEntity.class ).load( 1 );
		e.setName( "changed name" );
		session.createQuery("SELECT e FROM TheEntity e").getResultList();
		e.setName( "changed name again" );
		assertThat( e.getTs(), notNullValue() );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		e = session.byId( TheEntity.class ).load( 1 );
		session.delete( e );
		session.getTransaction().commit();
		session.close();
	}


	@Entity(name = "TheEntity")
	@Table(name="the_entity")
	public static class TheEntity {
		private Integer id;
		private Date ts;
		private String name;

		public TheEntity() {
		}

		public TheEntity(Integer id) {
			this.id = id;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Version
		public Date getTs() {
			return ts;
		}

		public void setTs(Date ts) {
			this.ts = ts;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
