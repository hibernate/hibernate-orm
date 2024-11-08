package org.hibernate.orm.test.bytecode.enhancement.access;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				InvalidPropertyNameTest.SomeEntity.class,
				InvalidPropertyNameTest.SomeEntityWithFalsePositive.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class InvalidPropertyNameTest {


	@Test
	@FailureExpected(jiraKey = "HHH-16572")
	@JiraKey("HHH-16572")
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new SomeEntity( 1L, "field", "property" ) );
		} );

		scope.inTransaction( session -> {
			SomeEntity entity = session.get( SomeEntity.class, 1L );
			assertThat( entity.property ).isEqualTo( "from getter: property" );

			entity.setPropertyMethod( "updated" );
		} );

		scope.inTransaction( session -> {
			SomeEntity entity = session.get( SomeEntity.class, 1L );
			assertThat( entity.property ).isEqualTo( "from getter: updated" );
		} );
	}

	@Test
	@JiraKey("HHH-18832")
	public void testNoFalsePositive(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new SomeEntityWithFalsePositive( 1L, "property1-initial", "property2-initial" ) );
		} );

		// Before HHH-18832 was fixed, lazy-loading enhancement was (incorrectly) skipped,
		// resulting at best in `property1` being null in the code below,
		// at worst in other errors such as java.lang.NoSuchMethodError: 'java.lang.String org.hibernate.orm.test.bytecode.enhancement.access.InvalidPropertyNameTest$SomeEntityWithFalsePositive.$$_hibernate_read_property1()'
		// (see https://hibernate.zulipchat.com/#narrow/channel/132094-hibernate-orm-dev/topic/HHH-16572/near/481330806)
		scope.inTransaction( session -> {
			SomeEntityWithFalsePositive entity = session.getReference( SomeEntityWithFalsePositive.class, 1L );
			// Lazy-loading triggered by field access
			// Proves bytecode enhancement is effective
			assertThat( entity.property1 ).isEqualTo( "property1-initial" );
		} );

		scope.inTransaction( session -> {
			SomeEntityWithFalsePositive entity = session.getReference( SomeEntityWithFalsePositive.class, 1L );
			// Proves bytecode enhancement is effective even for the transient method
			assertThat( entity.getProperty() ).isEqualTo( "property1-initial property2-initial" );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "delete from SomeEntity" ).executeUpdate();
			session.createQuery( "delete from SomeEntityWithFalsePositive" ).executeUpdate();
		} );
	}

	@Entity(name = "SomeEntity")
	@Table(name = "SOME_ENTITY")
	static class SomeEntity {
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

		/**
		 * The following property accessor methods are purposely named incorrectly to
		 * not match the "property" field.  The HHH-16572 change ensures that
		 * this entity is not (bytecode) enhanced.  Eventually further changes will be made
		 * such that this entity is enhanced in which case the FailureExpected can be removed
		 * and the cleanup() uncommented.
		 */
		@Basic
		@Access(AccessType.PROPERTY)
		public String getPropertyMethod() {
			return "from getter: " + property;
		}

		public void setPropertyMethod(String property) {
			this.property = property;
		}
	}

	@Entity(name = "SomeEntityWithFalsePositive")
	static class SomeEntityWithFalsePositive {

		private Long id;

		private String property1;

		private String property2;

		public SomeEntityWithFalsePositive() {
		}

		public SomeEntityWithFalsePositive(Long id, String property1, String property2) {
			this.id = id;
			this.property1 = property1;
			this.property2 = property2;
		}

		@Id
		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getProperty1() {
			return property1;
		}

		public void setProperty1(String property1) {
			this.property1 = property1;
		}

		public String getProperty2() {
			return property2;
		}

		public void setProperty2(String property2) {
			this.property2 = property2;
		}

		@Transient
		public String getProperty() {
			return property1 + " " + property2;
		}
	}
}
