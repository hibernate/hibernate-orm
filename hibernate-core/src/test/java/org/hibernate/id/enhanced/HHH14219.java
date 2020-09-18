/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.enhanced;

import static org.junit.Assert.assertEquals;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.query.Query;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.CustomRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@inheritDoc}
 *
 * @author Yanming Zhou
 */
@TestForIssue(jiraKey = "HHH-14219")
@RunWith(CustomRunner.class)
@RequiresDialect(MySQLDialect.class)
public class HHH14219 {

	private SessionFactory sf;

	@Before
	public void setup() {
		StandardServiceRegistryBuilder srb = new StandardServiceRegistryBuilder()

				.applySetting("hibernate.show_sql", "true").applySetting("hibernate.format_sql", "true")
				.applySetting("hibernate.hbm2ddl.auto", "create-drop");

		Metadata metadata = new MetadataSources(srb.build()).addAnnotatedClass(BaseEntity.class)
				.addAnnotatedClass(Foo.class).addAnnotatedClass(Bar.class).buildMetadata();

		sf = metadata.buildSessionFactory();
	}

	@Test
	public void testSequenceTableContainsOnlyOneRow() throws Exception {
		try (Session session = sf.openSession()) {
			@SuppressWarnings("unchecked")
			Query<Number> q = session.createNativeQuery("select count(*) from " + BaseEntity.SHARED_SEQ_NAME);
			assertEquals(1, q.uniqueResult().intValue());
		}
	}

	@MappedSuperclass
	public static class BaseEntity {

		public static final String SHARED_SEQ_NAME = "shared_seq";

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO, generator = SHARED_SEQ_NAME)
		protected Long id;

	}

	@Entity
	public static class Foo extends BaseEntity {

	}

	@Entity
	public static class Bar extends BaseEntity {

	}

}
