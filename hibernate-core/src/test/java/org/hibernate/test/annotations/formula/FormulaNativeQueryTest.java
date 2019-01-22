/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.formula;

import org.hibernate.annotations.Formula;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.List;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Алексей Макаров
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-7525")
public class FormulaNativeQueryTest extends BaseCoreFunctionalTestCase {

	// Add your entities here.
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Foo.class
		};
	}

	// Add in any settings that are specific to your test.  See resources/hibernate.properties for the defaults.
	@Override
	protected void configure(Configuration configuration) {
		super.configure(configuration);

		configuration.setProperty(AvailableSettings.SHOW_SQL, Boolean.TRUE.toString());
		configuration.setProperty(AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString());
	}

	@Before
	public void prepare() {
		doInHibernate(
				this::sessionFactory, session -> {
					session.persist( new Foo( 1, 1 ) );
					session.persist( new Foo( 1, 2 ) );
					session.persist( new Foo( 2, 1 ) );
				}
		);
	}

	@After
	public void cleanup() {
		doInHibernate(
				this::sessionFactory, session -> {
					session.createQuery( "delete from Foo" ).executeUpdate();
				}
		);
	}

	@Test
	@FailureExpected( jiraKey = "HHH-7525" )
	public void testNativeQuery() throws Exception {
		doInHibernate(
				this::sessionFactory, session -> {
					Query query = session.createNativeQuery( "SELECT ft.* FROM foo_table ft", Foo.class );
					List<Foo> list = query.getResultList();
					assertEquals( 3, list.size() );
				}
		);
	}

	@Test
	public void testHql() throws Exception {
		// Show that HQL does work
		doInHibernate(
				this::sessionFactory, session -> {
					Query query = session.createQuery( "SELECT ft FROM Foo ft", Foo.class );
					List<Foo> list = query.getResultList();
					assertEquals(3, list.size());
				}
		);
	}

	@Entity(name = "Foo")
	@Table(name = "foo_table")
	public static class Foo {

		private int id;
		private int locationStart;
		private int locationEnd;
		private int distance;

		public Foo() {
		}

		public Foo(int locationStart, int locationEnd) {
			this.locationStart = locationStart;
			this.locationEnd = locationEnd;
		}

		@Id
		@GeneratedValue
		public int getId() {
			return id;
		}
		private void setId(int id) {
			this.id = id;
		}

		public int getLocationStart() {
			return locationStart;
		}
		public void setLocationStart(int locationStart) {
			this.locationStart = locationStart;
		}

		public int getLocationEnd() {
			return locationEnd;
		}
		public void setLocationEnd(int locationEnd) {
			this.locationEnd = locationEnd;
		}

		@Formula("abs(locationEnd - locationStart)")
		public int getDistance() {
			return distance;
		}

		public void setDistance(int distance) {
			this.distance = distance;
		}
	}
}
