/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey(value = "HHH-15022")
@RequiresDialect(PostgreSQLDialect.class)
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
			assertThat( session.createQuery( "select count(*) from superent", Long.class ).uniqueResult() )
					.isEqualTo( 2L );
		} );
		inTransaction( session -> {
			session.createMutationQuery( "delete from subent1" ).executeUpdate();
		} );
		inTransaction( session -> {
			assertThat( session.createQuery( "select count(*) from superent", Long.class ).uniqueResult() )
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
			super(id, superProperty);
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
			super(id, superProperty);
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
