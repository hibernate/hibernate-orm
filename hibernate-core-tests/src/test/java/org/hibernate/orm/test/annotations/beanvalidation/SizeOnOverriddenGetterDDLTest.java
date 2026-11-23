package org.hibernate.orm.test.annotations.beanvalidation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An overridden getter inherits the {@code @Size} of the super getter, so several
 * {@code @Size} constraints apply to the same property. The column length must be the
 * most restrictive one, independent of the iteration order of the constraint descriptors.
 */
@ServiceRegistry(settings = @Setting(name = SchemaToolingSettings.APPLY_VALIDATION_CONSTRAINTS, value = "REQUIRED"))
@DomainModel(annotatedClasses = {
		SizeOnOverriddenGetterDDLTest.Base.class,
		SizeOnOverriddenGetterDDLTest.Derived.class
})
@SessionFactory
class SizeOnOverriddenGetterDDLTest {

	@Test
	void testMostRestrictiveSizeWins(SessionFactoryScope scope) {
		scope.getSessionFactory();
		final var column = scope.getMetadataImplementor()
				.getEntityBinding( Derived.class.getName() )
				.getProperty( "name" )
				.getColumns()
				.get( 0 );
		assertThat( column.getLength() ).isEqualTo( 20L );
	}

	@MappedSuperclass
	public abstract static class Base {
		private String name;

		@Size(max = 50)
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Derived")
	public static class Derived extends Base {
		private Long id;

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Size(max = 20)
		@Override
		public String getName() {
			return super.getName();
		}
	}
}
