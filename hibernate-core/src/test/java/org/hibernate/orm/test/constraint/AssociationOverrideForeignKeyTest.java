package org.hibernate.orm.test.constraint;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static jakarta.persistence.ConstraintMode.NO_CONSTRAINT;
import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-20763")
@RequiresDialect(H2Dialect.class)
@DomainModel(annotatedClasses = {
		AssociationOverrideForeignKeyTest.Target.class,
		AssociationOverrideForeignKeyTest.NestedNoConstraint.class,
		AssociationOverrideForeignKeyTest.NestedNamedConstraint.class,
		AssociationOverrideForeignKeyTest.OuterNoConstraint.class,
		AssociationOverrideForeignKeyTest.OuterNamedConstraint.class,
		AssociationOverrideForeignKeyTest.DefaultConstraint.class
})
@SessionFactory
class AssociationOverrideForeignKeyTest {
	@Test
	void nestedNoConstraint(SessionFactoryScope scope) {
		assertThat( importedKeys( scope, "JC_NESTED_NONE" ) ).isEmpty();
	}

	@Test
	void nestedNamedConstraint(SessionFactoryScope scope) {
		assertThat( importedKeys( scope, "JC_NESTED_NAMED" ) ).containsExactly( "FK_JC_NESTED" );
	}

	@Test
	void outerNoConstraint(SessionFactoryScope scope) {
		assertThat( importedKeys( scope, "JC_OUTER_NONE" ) ).isEmpty();
	}

	@Test
	void outerNamedConstraint(SessionFactoryScope scope) {
		assertThat( importedKeys( scope, "JC_OUTER_NAMED" ) ).containsExactly( "FK_JC_OUTER" );
	}

	@Test
	void defaultOverrideDoesNotReuseInheritedNoConstraint(SessionFactoryScope scope) {
		assertThat( importedKeys( scope, "JC_OVERRIDE_DEFAULT" ) ).hasSize( 1 );
	}

	private List<String> importedKeys(SessionFactoryScope scope, String tableName) {
		final List<String> names = new ArrayList<>();
		scope.inSession( session -> session.doWork( connection -> {
			try ( var keys = connection.getMetaData().getImportedKeys( null, null, tableName ) ) {
				while ( keys.next() ) {
					names.add( keys.getString( "FK_NAME" ) );
				}
			}
		} ) );
		return names;
	}

	@Entity(name = "OverrideForeignKeyTarget")
	@Table(name = "fk_target")
	static class Target {
		@Id
		Integer id;
	}

	@MappedSuperclass
	static class OwnerBase {
		@Id
		Integer id;

		@OneToOne(optional = false)
		@JoinColumn(name = "id", insertable = false, updatable = false,
				foreignKey = @ForeignKey(NO_CONSTRAINT))
		Target target;
	}

	@Entity(name = "OverrideNestedNoConstraint")
	@Table(name = "jc_nested_none")
	@AssociationOverride(name = "target", joinColumns =
			@JoinColumn(name = "id", insertable = false, updatable = false,
					foreignKey = @ForeignKey(NO_CONSTRAINT)))
	static class NestedNoConstraint extends OwnerBase {
	}

	@Entity(name = "OverrideNestedNamedConstraint")
	@Table(name = "jc_nested_named")
	@AssociationOverride(name = "target", joinColumns =
			@JoinColumn(name = "id", insertable = false, updatable = false,
					foreignKey = @ForeignKey(name = "FK_JC_NESTED")))
	static class NestedNamedConstraint extends OwnerBase {
	}

	@Entity(name = "OverrideOuterNoConstraint")
	@Table(name = "jc_outer_none")
	@AssociationOverride(name = "target", joinColumns =
			@JoinColumn(name = "id", insertable = false, updatable = false),
			foreignKey = @ForeignKey(NO_CONSTRAINT))
	static class OuterNoConstraint extends OwnerBase {
	}

	@Entity(name = "OverrideOuterNamedConstraint")
	@Table(name = "jc_outer_named")
	@AssociationOverride(name = "target", joinColumns =
			@JoinColumn(name = "id", insertable = false, updatable = false),
			foreignKey = @ForeignKey(name = "FK_JC_OUTER"))
	static class OuterNamedConstraint extends OwnerBase {
	}

	@Entity(name = "OverrideDefaultConstraint")
	@Table(name = "jc_override_default")
	@AssociationOverride(name = "target", joinColumns =
			@JoinColumn(name = "id", insertable = false, updatable = false))
	static class DefaultConstraint extends OwnerBase {
	}
}
