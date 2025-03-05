/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ValidationMode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.hibernate.SessionFactory;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Jpa(annotatedClasses = {
		CollectionActionsValidationTest.TestEntity.class,
		CollectionActionsValidationTest.ChildEntity.class,
}, validationMode = ValidationMode.AUTO)
@Jira( "https://hibernate.atlassian.net/browse/HHH-19232" )
public class CollectionActionsValidationTest {
	@Test
	public void testPersistEmpty(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final ConstraintViolationException e = assertThrows( ConstraintViolationException.class, () -> {
				final TestEntity entity = new TestEntity( 2L );
				assertThat( entity.getNotEmptySet() ).isNull();
				assertThat( entity.getMinSizeList() ).isNull();
				entityManager.persist( entity );
				entityManager.flush();
			} );
			assertThat( e.getConstraintViolations() ).hasSize( 1 );
			assertThat( getPropertyPaths( e ) ).containsOnly( "notEmptySet" );
		} );
	}

	@Test
	public void testPersistInvalidChild(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final ConstraintViolationException e = assertThrows( ConstraintViolationException.class, () -> {
				final TestEntity entity = new TestEntity( 2L );
				entity.setNotEmptySet( Set.of( new ChildEntity( 2L, "" ) ) );
				entity.setMinSizeList( List.of( "test" ) );
				entityManager.persist( entity );
				entityManager.flush();
			} );
			assertThat( e.getConstraintViolations() ).hasSize( 1 );
			assertThat( getPropertyPaths( e ) ).containsOnly( "name" );
		} );
	}

	@Test
	public void testUpdateEmptyUsingGetter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final ConstraintViolationException e = assertThrows( ConstraintViolationException.class, () -> {
				final TestEntity entity = entityManager.find( TestEntity.class, 1L );
				entity.getNotEmptySet().clear();
				entityManager.flush();
			} );
			assertThat( e.getConstraintViolations() ).hasSize( 1 );
			assertThat( getPropertyPaths( e ) ).containsOnly( "notEmptySet" );

			entityManager.clear();

			final ConstraintViolationException e2 = assertThrows( ConstraintViolationException.class, () -> {
				final TestEntity entity = entityManager.find( TestEntity.class, 1L );
				entity.getMinSizeList().clear();
				entityManager.flush();
			} );
			assertThat( e2.getConstraintViolations() ).hasSize( 1 );
			assertThat( getPropertyPaths( e2 ) ).containsOnly( "minSizeList" );
		} );
	}

	@Test
	public void testUpdateEmptyUsingSetter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final ConstraintViolationException e = assertThrows( ConstraintViolationException.class, () -> {
				final TestEntity entity = entityManager.find( TestEntity.class, 1L );
				entity.setNotEmptySet( Set.of() );
				entityManager.flush();
			} );
			assertThat( e.getConstraintViolations() ).hasSize( 1 );
			assertThat( getPropertyPaths( e ) ).containsOnly( "notEmptySet" );

			entityManager.clear();

			final ConstraintViolationException e2 = assertThrows( ConstraintViolationException.class, () -> {
				final TestEntity entity = entityManager.find( TestEntity.class, 1L );
				entity.setMinSizeList( List.of() );
				entityManager.flush();
			} );
			assertThat( e2.getConstraintViolations() ).hasSize( 1 );
			assertThat( getPropertyPaths( e2 ) ).containsOnly( "minSizeList" );
		} );
	}

	@Test
	public void testUpdateNull(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final TestEntity entity = new TestEntity( 3L );
			entity.setNotEmptySet( Set.of( new ChildEntity( 3L, "child_3" ) ) );
			entity.setMinSizeList( List.of( "three" ) );
			entityManager.persist( entity );
		} );
		scope.inTransaction( entityManager -> {
			final ConstraintViolationException e = assertThrows( ConstraintViolationException.class, () -> {
				final TestEntity entity = entityManager.find( TestEntity.class, 3L );
				entity.setNotEmptySet( null );
				entityManager.flush();
			} );
			assertThat( e.getConstraintViolations() ).hasSize( 1 );
			assertThat( getPropertyPaths( e ) ).containsOnly( "notEmptySet" );
		} );
	}

	@Test
	public void testUpdateInvalidChild(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final ConstraintViolationException e = assertThrows( ConstraintViolationException.class, () -> {
				final TestEntity entity = entityManager.find( TestEntity.class, 1L );
				final ChildEntity child = entity.getNotEmptySet().iterator().next();
				child.setName( "" );
				entityManager.flush();
			} );
			assertThat( e.getConstraintViolations() ).hasSize( 1 );
			assertThat( getPropertyPaths( e ) ).containsOnly( "name" );
		} );
	}

	@Test
	public void testUpdateCollectionUsingGetterAndBasicProperty(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final ConstraintViolationException e = assertThrows( ConstraintViolationException.class, () -> {
				final TestEntity entity = entityManager.find( TestEntity.class, 1L );
				entity.getNotEmptySet().clear();
				entity.setExpiryDate( LocalDate.now().minusDays( 1L ) );
				entityManager.flush();
			} );
			assertThat( e.getConstraintViolations() ).hasSize( 2 );
			assertThat( getPropertyPaths( e ) ).containsOnly( "notEmptySet", "expiryDate" );
		} );
	}

	private static List<String> getPropertyPaths(ConstraintViolationException e) {
		return e.getConstraintViolations().stream().map( ConstraintViolation::getPropertyPath ).map( Path::toString )
				.collect( Collectors.toList() );
	}

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final TestEntity a = new TestEntity( 1L );
			a.setNotEmptySet( Set.of( new ChildEntity( 1L, "child_1" ) ) );
			a.setMinSizeList( List.of( "one" ) );
			entityManager.persist( a );
		} );
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().unwrap( SessionFactory.class ).getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "TestEntity")
	static class TestEntity {
		@Id
		private Long id;

		@ManyToMany(cascade = CascadeType.PERSIST)
		@NotEmpty
		private Set<ChildEntity> notEmptySet;

		@ElementCollection
		@Size(min = 1)
		private List<String> minSizeList;

		@Future
		private LocalDate expiryDate = LocalDate.now().plusMonths( 1L );

		public TestEntity() {
		}

		public TestEntity(Long id) {
			this.id = id;
		}

		public Set<ChildEntity> getNotEmptySet() {
			return notEmptySet;
		}

		public void setNotEmptySet(Set<ChildEntity> notEmptySet) {
			this.notEmptySet = notEmptySet;
		}

		public List<String> getMinSizeList() {
			return minSizeList;
		}

		public void setMinSizeList(List<String> minSizeList) {
			this.minSizeList = minSizeList;
		}

		public LocalDate getExpiryDate() {
			return expiryDate;
		}

		public void setExpiryDate(LocalDate updateDate) {
			this.expiryDate = updateDate;
		}
	}

	@Entity(name = "ChildEntity")
	static class ChildEntity {
		@Id
		private Long id;

		@NotBlank
		private String name;

		public ChildEntity() {
		}

		public ChildEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
