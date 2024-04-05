package org.hibernate.orm.test.bytecode.enhancement.access;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@DomainModel(
		annotatedClasses = {
				PropertyAccessTest.SomeEntity.class,
		}
)
@SessionFactory
@JiraKey("HHH-16799")
@BytecodeEnhanced
public class PropertyAccessTest {


	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new SomeEntity( 1L, "field", "property" ) );
		} );

		scope.inTransaction( session -> {
			SomeEntity entity = session.get( SomeEntity.class, 1L );
			assertThat( entity.property ).isEqualTo( "from getter: property" );

			entity.setProperty( "updated" );
		} );

		scope.inTransaction( session -> {
			SomeEntity entity = session.get( SomeEntity.class, 1L );
			assertThat( entity.property ).isEqualTo( "from getter: updated" );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.remove( session.get( SomeEntity.class, 1L ) );
		} );
	}

	@Entity
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
