package org.hibernate.orm.test.constraint;

import java.util.Collection;

import org.hibernate.mapping.ForeignKey;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;
import jakarta.persistence.Table;

import static jakarta.persistence.ConstraintMode.CONSTRAINT;
import static jakarta.persistence.ConstraintMode.NO_CONSTRAINT;
import static jakarta.persistence.FetchType.LAZY;
import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-20763")
@DomainModel(annotatedClasses = {
		PrimaryKeyJoinColumnForeignKeyTest.Target.class,
		PrimaryKeyJoinColumnForeignKeyTest.NoConstraint.class,
		PrimaryKeyJoinColumnForeignKeyTest.DefaultConstraint.class,
		PrimaryKeyJoinColumnForeignKeyTest.NamedConstraint.class,
		PrimaryKeyJoinColumnForeignKeyTest.ContainerNoConstraint.class,
		PrimaryKeyJoinColumnForeignKeyTest.ContainerNamedConstraint.class,
		PrimaryKeyJoinColumnForeignKeyTest.NestedNamedConstraint.class,
		PrimaryKeyJoinColumnForeignKeyTest.JoinColumnNamedConstraint.class,
		PrimaryKeyJoinColumnForeignKeyTest.JoinColumnNoConstraint.class,
		PrimaryKeyJoinColumnForeignKeyTest.OverriddenConstraint.class,
		PrimaryKeyJoinColumnForeignKeyTest.OverriddenDefaultConstraint.class,
		PrimaryKeyJoinColumnForeignKeyTest.NestedNoConstraint.class,
		PrimaryKeyJoinColumnForeignKeyTest.CompositeTarget.class,
		PrimaryKeyJoinColumnForeignKeyTest.SecondColumnNamedConstraint.class,
		PrimaryKeyJoinColumnForeignKeyTest.SecondColumnNoConstraint.class,
		PrimaryKeyJoinColumnForeignKeyTest.SecondColumnDefinition.class,
		PrimaryKeyJoinColumnForeignKeyTest.SecondColumnOptions.class,
		PrimaryKeyJoinColumnForeignKeyTest.ManyToOneWithPrimaryKeyJoinColumn.class
})
@SessionFactory
class PrimaryKeyJoinColumnForeignKeyTest {
	@BeforeAll
	void exportSchema(SessionFactoryScope scope) {
		scope.getSessionFactory();
	}

	@Test
	void testNoConstraint(SessionFactoryScope scope) {
		assertThat( foreignKeys( scope, NoConstraint.class ) ).isEmpty();
	}

	@Test
	void testDefaultConstraint(SessionFactoryScope scope) {
		assertThat( foreignKeys( scope, DefaultConstraint.class ) ).hasSize( 1 );
	}

	@Test
	void testNamedConstraint(SessionFactoryScope scope) {
		assertThat( foreignKeys( scope, NamedConstraint.class ) )
				.extracting( ForeignKey::getName ).containsExactly( "MY_FK_CUSTOM_NAME" );
	}

	@Test
	void testContainerNoConstraint(SessionFactoryScope scope) {
		assertThat( foreignKeys( scope, ContainerNoConstraint.class ) ).isEmpty();
	}

	@Test
	void testContainerNamedConstraint(SessionFactoryScope scope) {
		assertThat( foreignKeys( scope, ContainerNamedConstraint.class ) )
				.extracting( ForeignKey::getName ).containsExactly( "FK_PK_CONTAINER" );
	}

	@Test
	void testNestedNamedConstraint(SessionFactoryScope scope) {
		assertThat( foreignKeys( scope, NestedNamedConstraint.class ) )
				.extracting( ForeignKey::getName ).containsExactly( "FK_PK_NESTED" );
	}

	@Test
	void testJoinColumnNamedConstraint(SessionFactoryScope scope) {
		assertThat( foreignKeys( scope, JoinColumnNamedConstraint.class ) )
				.extracting( ForeignKey::getName ).containsExactly( "FK_JOIN_COLUMN" );
	}

	@Test
	void testJoinColumnNoConstraint(SessionFactoryScope scope) {
		assertThat( foreignKeys( scope, JoinColumnNoConstraint.class ) ).isEmpty();
	}

	@Test
	void testAssociationOverrideConstraint(SessionFactoryScope scope) {
		assertThat( foreignKeys( scope, OverriddenConstraint.class ) ).hasSize( 1 );
	}

	@Test
	void testAssociationOverrideDefaultConstraint(SessionFactoryScope scope) {
		assertThat( foreignKeys( scope, OverriddenDefaultConstraint.class ) ).hasSize( 1 );
	}

	@Test
	void testNestedNoConstraint(SessionFactoryScope scope) {
		assertThat( foreignKeys( scope, NestedNoConstraint.class ) ).isEmpty();
	}

	@Test
	void testSecondColumnNamedConstraint(SessionFactoryScope scope) {
		assertThat( foreignKeys( scope, SecondColumnNamedConstraint.class ) )
				.extracting( ForeignKey::getName ).containsExactly( "FK_PK_SECOND_COLUMN" );
	}

	@Test
	void testSecondColumnNoConstraint(SessionFactoryScope scope) {
		assertThat( foreignKeys( scope, SecondColumnNoConstraint.class ) ).isEmpty();
	}

	@Test
	void testSecondColumnDefinition(SessionFactoryScope scope) {
		assertThat( foreignKeys( scope, SecondColumnDefinition.class ) )
				.extracting( ForeignKey::getKeyDefinition )
				.containsExactly( "foreign key (first_id, second_id) references pk_fk_composite_target" );
	}

	@Test
	void testSecondColumnOptions(SessionFactoryScope scope) {
		assertThat( foreignKeys( scope, SecondColumnOptions.class ) )
				.extracting( ForeignKey::getOptions ).containsExactly( "on delete cascade" );
	}

	@Test
	void testManyToOneIgnoresPrimaryKeyJoinColumn(SessionFactoryScope scope) {
		assertThat( foreignKeys( scope, ManyToOneWithPrimaryKeyJoinColumn.class ) ).hasSize( 1 );
	}

	private Collection<ForeignKey> foreignKeys(SessionFactoryScope scope, Class<?> entityClass) {
		return scope.getMetadataImplementor().getEntityBinding( entityClass.getName() )
				.getTable().getForeignKeyCollection();
	}

	@Entity(name = "PrimaryKeyForeignKeyTarget")
	@Table(name = "pk_fk_target")
	static class Target {
		@Id
		Integer id;
	}

	@Entity(name = "PrimaryKeyNoConstraint")
	@Table(name = "pk_fk_none")
	static class NoConstraint {
		@Id
		Integer id;

		@OneToOne(fetch = LAZY, optional = false)
		@PrimaryKeyJoinColumn(foreignKey = @jakarta.persistence.ForeignKey(NO_CONSTRAINT))
		Target target;
	}

	@Entity(name = "PrimaryKeyDefaultConstraint")
	@Table(name = "pk_fk_default")
	static class DefaultConstraint {
		@Id
		Integer id;

		@OneToOne(fetch = LAZY, optional = false)
		@PrimaryKeyJoinColumn
		Target target;
	}

	@Entity(name = "PrimaryKeyNamedConstraint")
	@Table(name = "pk_fk_named")
	static class NamedConstraint {
		@Id
		Integer id;

		@OneToOne(fetch = LAZY, optional = false)
		@PrimaryKeyJoinColumn(foreignKey = @jakarta.persistence.ForeignKey(
				name = "MY_FK_CUSTOM_NAME", value = CONSTRAINT))
		Target target;
	}

	@Entity(name = "PrimaryKeyContainerNoConstraint")
	@Table(name = "pk_fk_container_none")
	static class ContainerNoConstraint {
		@Id
		Integer id;

		@OneToOne(fetch = LAZY, optional = false)
		@PrimaryKeyJoinColumns(
				value = @PrimaryKeyJoinColumn,
				foreignKey = @jakarta.persistence.ForeignKey(NO_CONSTRAINT))
		Target target;
	}

	@Entity(name = "PrimaryKeyContainerNamedConstraint")
	@Table(name = "pk_fk_container_named")
	static class ContainerNamedConstraint {
		@Id
		Integer id;

		@OneToOne(fetch = LAZY, optional = false)
		@PrimaryKeyJoinColumns(
				value = @PrimaryKeyJoinColumn,
				foreignKey = @jakarta.persistence.ForeignKey(name = "FK_PK_CONTAINER"))
		Target target;
	}

	@Entity(name = "PrimaryKeyNestedNamedConstraint")
	@Table(name = "pk_fk_nested_named")
	static class NestedNamedConstraint {
		@Id
		Integer id;

		@OneToOne(fetch = LAZY, optional = false)
		@PrimaryKeyJoinColumns(@PrimaryKeyJoinColumn(
				foreignKey = @jakarta.persistence.ForeignKey(name = "FK_PK_NESTED")))
		Target target;
	}

	@Entity(name = "JoinColumnNamedConstraint")
	@Table(name = "pk_fk_join_named")
	static class JoinColumnNamedConstraint {
		@Id
		Integer id;

		@OneToOne(fetch = LAZY, optional = false)
		@JoinColumn(name = "target_id", foreignKey = @jakarta.persistence.ForeignKey(name = "FK_JOIN_COLUMN"))
		Target target;
	}

	@Entity(name = "JoinColumnNoConstraint")
	@Table(name = "pk_fk_join_none")
	static class JoinColumnNoConstraint {
		@Id
		Integer id;

		@OneToOne(fetch = LAZY, optional = false)
		@JoinColumn(name = "target_id", foreignKey = @jakarta.persistence.ForeignKey(NO_CONSTRAINT))
		Target target;
	}

	@MappedSuperclass
	static class InheritedAssociation {
		@Id
		Integer id;

		@OneToOne(fetch = LAZY, optional = false)
		@PrimaryKeyJoinColumn(foreignKey = @jakarta.persistence.ForeignKey(NO_CONSTRAINT))
		Target target;
	}

	@Entity(name = "OverriddenPrimaryKeyConstraint")
	@Table(name = "pk_fk_override")
	@AssociationOverride(name = "target", joinColumns = @JoinColumn(name = "id"),
			foreignKey = @jakarta.persistence.ForeignKey(CONSTRAINT))
	static class OverriddenConstraint extends InheritedAssociation {
	}

	@Entity(name = "OverriddenPrimaryKeyDefaultConstraint")
	@Table(name = "pk_fk_override_default")
	@AssociationOverride(name = "target", joinColumns = @JoinColumn(name = "id"))
	static class OverriddenDefaultConstraint extends InheritedAssociation {
	}

	@Entity(name = "PrimaryKeyNestedNoConstraint")
	@Table(name = "pk_fk_nested_none")
	static class NestedNoConstraint {
		@Id
		Integer id;

		@OneToOne(fetch = LAZY, optional = false)
		@PrimaryKeyJoinColumns(@PrimaryKeyJoinColumn(
				foreignKey = @jakarta.persistence.ForeignKey(NO_CONSTRAINT)))
		Target target;
	}

	@Embeddable
	record CompositeId(@Column(name = "first_id") Integer first, @Column(name = "second_id") Integer second) {
	}

	@Entity(name = "CompositePrimaryKeyForeignKeyTarget")
	@Table(name = "pk_fk_composite_target")
	static class CompositeTarget {
		@EmbeddedId
		CompositeId id;
	}

	@Entity(name = "PrimaryKeySecondColumnNamedConstraint")
	@Table(name = "pk_fk_second_named")
	static class SecondColumnNamedConstraint {
		@EmbeddedId
		CompositeId id;

		@OneToOne(fetch = LAZY, optional = false)
		@PrimaryKeyJoinColumns({
				@PrimaryKeyJoinColumn(name = "first_id", referencedColumnName = "first_id"),
				@PrimaryKeyJoinColumn(name = "second_id", referencedColumnName = "second_id",
						foreignKey = @jakarta.persistence.ForeignKey(name = "FK_PK_SECOND_COLUMN"))
		})
		CompositeTarget target;
	}

	@Entity(name = "PrimaryKeySecondColumnNoConstraint")
	@Table(name = "pk_fk_second_none")
	static class SecondColumnNoConstraint {
		@EmbeddedId
		CompositeId id;

		@OneToOne(fetch = LAZY, optional = false)
		@PrimaryKeyJoinColumns({
				@PrimaryKeyJoinColumn(name = "first_id", referencedColumnName = "first_id"),
				@PrimaryKeyJoinColumn(name = "second_id", referencedColumnName = "second_id",
						foreignKey = @jakarta.persistence.ForeignKey(NO_CONSTRAINT))
		})
		CompositeTarget target;
	}

	@Entity(name = "PrimaryKeySecondColumnDefinition")
	@Table(name = "pk_fk_second_definition")
	static class SecondColumnDefinition {
		@EmbeddedId
		CompositeId id;

		@OneToOne(fetch = LAZY, optional = false)
		@PrimaryKeyJoinColumns({
				@PrimaryKeyJoinColumn(name = "first_id", referencedColumnName = "first_id"),
				@PrimaryKeyJoinColumn(name = "second_id", referencedColumnName = "second_id",
						foreignKey = @jakarta.persistence.ForeignKey(foreignKeyDefinition = "foreign key (first_id, second_id) references pk_fk_composite_target"))
		})
		CompositeTarget target;
	}

	@Entity(name = "PrimaryKeySecondColumnOptions")
	@Table(name = "pk_fk_second_options")
	static class SecondColumnOptions {
		@EmbeddedId
		CompositeId id;

		@OneToOne(fetch = LAZY, optional = false)
		@PrimaryKeyJoinColumns({
				@PrimaryKeyJoinColumn(name = "first_id", referencedColumnName = "first_id"),
				@PrimaryKeyJoinColumn(name = "second_id", referencedColumnName = "second_id",
						foreignKey = @jakarta.persistence.ForeignKey(options = "on delete cascade"))
		})
		CompositeTarget target;
	}

	@Entity(name = "ManyToOneWithPrimaryKeyJoinColumn")
	@Table(name = "pk_fk_many_to_one")
	static class ManyToOneWithPrimaryKeyJoinColumn {
		@Id
		Integer id;

		@ManyToOne(fetch = LAZY, optional = false)
		@PrimaryKeyJoinColumn(foreignKey = @jakarta.persistence.ForeignKey(NO_CONSTRAINT))
		Target target;
	}
}
