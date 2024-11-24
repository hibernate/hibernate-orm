/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.PostgreSQL81Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-15022")
@RequiresDialect(PostgreSQL81Dialect.class)
public class DeleteAllWithTablePerClassAndDefaultSchemaTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SuperEntity.class, SubEntity1.class, SubEntity2.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.DEFAULT_SCHEMA, "public" );
	}

	@Before
	public void setUp() {
		inTransaction( session -> {
			SuperEntity entity1 = new SubEntity1( 1L, "super1", "sub1" );
			SuperEntity entity2 = new SubEntity2( 2L, "super2", "sub2" );
			session.persist( entity1 );
			session.persist( entity2 );
		} );
	}

	@Test
	public void testDeleteAll() {
		inTransaction( session -> {
			assertThat( session.createQuery( "select count(*) from superent" ).uniqueResult() )
					.isEqualTo( 2L );
		} );
		inTransaction( session -> {
			session.createQuery( "delete from subent1" ).executeUpdate();
		} );
		inTransaction( session -> {
			assertThat( session.createQuery( "select count(*) from superent" ).uniqueResult() )
					.isEqualTo( 1L );
		} );
	}

	@Entity(name = "superent")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public abstract static class SuperEntity {
		@Id
		private Long id;

		private String superProperty;

		public SuperEntity() {
		}

		public SuperEntity(Long id, String superProperty) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

	}

	@Entity(name = "subent1")
	public static class SubEntity1 extends SuperEntity {
		private String subProperty1;

		public SubEntity1() {
		}

		public SubEntity1(Long id, String superProperty, String subProperty) {
			super( id, superProperty );
			this.subProperty1 = subProperty;
		}

		public String getSubProperty1() {
			return subProperty1;
		}

		public void setSubProperty1(String subProperty1) {
			this.subProperty1 = subProperty1;
		}
	}

	@Entity(name = "subent2")
	public static class SubEntity2 extends SuperEntity {
		private String subProperty2;

		public SubEntity2() {
		}

		public SubEntity2(Long id, String superProperty, String subProperty) {
			super( id, superProperty );
			this.subProperty2 = subProperty;
		}

		public String getSubProperty2() {
			return subProperty2;
		}

		public void setSubProperty2(String subProperty2) {
			this.subProperty2 = subProperty2;
		}
	}

}
