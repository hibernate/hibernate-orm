package org.hibernate.orm.test.constraint;

import java.util.Collection;

import org.hibernate.mapping.ForeignKey;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
		PrimaryKeyJoinColumnForeignKeyTest.JoinColumnNoConstraint.class
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
}
