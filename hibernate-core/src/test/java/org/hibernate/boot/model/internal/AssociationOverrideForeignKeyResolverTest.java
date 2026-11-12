package org.hibernate.boot.model.internal;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;

import static jakarta.persistence.ConstraintMode.NO_CONSTRAINT;
import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-20763")
class AssociationOverrideForeignKeyResolverTest {
	@Test
	void secondColumnName() {
		assertSecondColumnSelected( Named.class );
	}

	@Test
	void secondColumnNoConstraint() {
		assertSecondColumnSelected( Suppressed.class );
	}

	@Test
	void secondColumnDefinition() {
		assertSecondColumnSelected( Defined.class );
	}

	@Test
	void secondColumnOptions() {
		assertSecondColumnSelected( WithOptions.class );
	}

	private static void assertSecondColumnSelected(Class<?> mapping) {
		final var override = mapping.getAnnotation( AssociationOverride.class );
		assertThat( BinderHelper.resolveAssociationOverrideForeignKey(
				override.foreignKey(), override.joinColumns() ) )
				.isEqualTo( override.joinColumns()[1].foreignKey() );
	}

	@AssociationOverride(name = "target", joinColumns = {
			@JoinColumn(name = "first_id"),
			@JoinColumn(name = "second_id", foreignKey = @ForeignKey(name = "FK_SECOND"))
	})
	static class Named {
	}

	@AssociationOverride(name = "target", joinColumns = {
			@JoinColumn(name = "first_id"),
			@JoinColumn(name = "second_id", foreignKey = @ForeignKey(NO_CONSTRAINT))
	})
	static class Suppressed {
	}

	@AssociationOverride(name = "target", joinColumns = {
			@JoinColumn(name = "first_id"),
			@JoinColumn(name = "second_id", foreignKey = @ForeignKey(foreignKeyDefinition = "foreign key (first_id, second_id) references target (first_id, second_id)"))
	})
	static class Defined {
	}

	@AssociationOverride(name = "target", joinColumns = {
			@JoinColumn(name = "first_id"),
			@JoinColumn(name = "second_id", foreignKey = @ForeignKey(options = "DEFERRABLE"))
	})
	static class WithOptions {
	}
}
