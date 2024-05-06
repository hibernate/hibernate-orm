package org.hibernate.orm.test.bytecode.enhancement.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@JiraKey("HHH-16799")
@RunWith(BytecodeEnhancerRunner.class)
public class PropertyAccessTest extends BaseCoreFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SomeEntity.class };
	}

	@Test
	public void test() {
		doInHibernate( this::sessionFactory, session -> {
			session.persist( new SomeEntity( 1L, "field", "property" ) );
		} );

		doInHibernate( this::sessionFactory, session -> {
			SomeEntity entity = session.get( SomeEntity.class, 1L );
			assertThat( entity.property ).isEqualTo( "from getter: property" );

			entity.setProperty( "updated" );
		} );

		doInHibernate( this::sessionFactory, session -> {
			SomeEntity entity = session.get( SomeEntity.class, 1L );
			assertThat( entity.property ).isEqualTo( "from getter: updated" );
		} );
	}

	@After
	public void cleanup() {
		doInHibernate( this::sessionFactory, session -> {
			session.remove( session.get( SomeEntity.class, 1L ) );
		} );
	}

	@Entity
	@Table(name = "SOME_ENTITY")
	private static class SomeEntity {
		@Id
		Long id;

		@Basic
		String field;

		String property;

		public SomeEntity() {
		}

		public SomeEntity(Long id, String field, String property) {
			this.id = id;
			this.field = field;
			this.property = property;
		}

		@Basic
		@Access(AccessType.PROPERTY)
		public String getProperty() {
			return "from getter: " + property;
		}

		public void setProperty(String property) {
			this.property = property;
		}
	}
}
