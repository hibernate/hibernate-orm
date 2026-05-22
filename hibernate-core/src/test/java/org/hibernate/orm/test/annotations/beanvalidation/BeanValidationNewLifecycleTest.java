/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.boot.beanvalidation.BeanValidationIntegrator;
import org.hibernate.cfg.ValidationSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for JPA 4.0 bean validation lifecycle event separation:
 * EM-level (persist, merge, remove) vs DB-level (stateless) (insert, update, upsert, delete).
 */
class BeanValidationNewLifecycleTest {

	public interface PersistGroup {}
	public interface InsertGroup {}
	public interface RemoveGroup {}
	public interface DeleteGroup {}

	@Entity(name = "DualValidatedItem")
	public static class DualValidatedItem {
		@Id
		@GeneratedValue
		private Integer id;

		@NotNull(groups = PersistGroup.class)
		private String persistField;

		@NotNull(groups = InsertGroup.class)
		private String insertField;

		@Size(min = 3, groups = RemoveGroup.class)
		private String removeField;

		@Size(min = 3, groups = DeleteGroup.class)
		private String deleteField;

		public Integer getId() {
			return id;
		}

		public String getPersistField() {
			return persistField;
		}

		public void setPersistField(String persistField) {
			this.persistField = persistField;
		}

		public String getInsertField() {
			return insertField;
		}

		public void setInsertField(String insertField) {
			this.insertField = insertField;
		}

		public String getRemoveField() {
			return removeField;
		}

		public void setRemoveField(String removeField) {
			this.removeField = removeField;
		}

		public String getDeleteField() {
			return deleteField;
		}

		public void setDeleteField(String deleteField) {
			this.deleteField = deleteField;
		}
	}

	@Entity(name = "ValidatedItem")
	public static class ValidatedItem {
		@Id
		@GeneratedValue
		private Integer id;

		@NotNull
		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	/**
	 * Default behavior: insert validates with Default group,
	 * persist does not validate (empty groups by default).
	 */
	@ServiceRegistry(settings = {
			@Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "callback"),
			@Setting(name = BeanValidationIntegrator.APPLY_CONSTRAINTS, value = "false"),
	})
	@DomainModel(annotatedClasses = ValidatedItem.class)
	@SessionFactory
	@Test
	void testInsertValidatesByDefault(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			var item = new ValidatedItem();
			// name is null, violates @NotNull
			assertThatThrownBy( () -> {
				s.persist( item );
				s.flush();
			} ).isInstanceOf( ConstraintViolationException.class );
		} );
	}

	/**
	 * When pre-insert is disabled but pre-persist is enabled,
	 * persist validation fires at insert time.
	 */
	@ServiceRegistry(settings = {
			@Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "callback"),
			@Setting(name = ValidationSettings.JAKARTA_INSERT_VALIDATION_GROUP, value = ""),
			@Setting(name = ValidationSettings.JAKARTA_PERSIST_VALIDATION_GROUP,
					value = "jakarta.validation.groups.Default"),
			@Setting(name = BeanValidationIntegrator.APPLY_CONSTRAINTS, value = "false"),
	})
	@DomainModel(annotatedClasses = ValidatedItem.class)
	@SessionFactory
	@Test
	void testPersistGroupValidation(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			var item = new ValidatedItem();
			// name is null, violates @NotNull -- persist group is configured
			assertThatThrownBy( () -> {
				s.persist( item );
				s.flush();
			} ).isInstanceOf( ConstraintViolationException.class );
		} );
	}

	/**
	 * When both pre-insert and pre-persist are disabled,
	 * no validation occurs on persist+flush.
	 */
	@ServiceRegistry(settings = {
			@Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "callback"),
			@Setting(name = ValidationSettings.JAKARTA_INSERT_VALIDATION_GROUP, value = ""),
			@Setting(name = ValidationSettings.JAKARTA_PERSIST_VALIDATION_GROUP, value = ""),
			@Setting(name = ValidationSettings.JAKARTA_UPDATE_VALIDATION_GROUP, value = ""),
			@Setting(name = BeanValidationIntegrator.APPLY_CONSTRAINTS, value = "false"),
	})
	@DomainModel(annotatedClasses = ValidatedItem.class)
	@SessionFactory
	@Test
	void testNoValidationWhenBothDisabled(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			var item = new ValidatedItem();
			// name is null, but no validation groups configured
			s.persist( item );
			s.flush();
		} );
	}

	/**
	 * Merge validation fires when pre-merge group is configured.
	 */
	@ServiceRegistry(settings = {
			@Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "callback"),
			@Setting(name = ValidationSettings.JAKARTA_MERGE_VALIDATION_GROUP,
					value = "jakarta.validation.groups.Default"),
			@Setting(name = BeanValidationIntegrator.APPLY_CONSTRAINTS, value = "false"),
	})
	@DomainModel(annotatedClasses = ValidatedItem.class)
	@SessionFactory
	@Test
	void testMergeGroupValidation(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			var item = new ValidatedItem();
			item.setName( "valid" );
			s.persist( item );
			s.flush();

			item.setName( null );
			s.detach( item );

			assertThatThrownBy( () -> s.merge( item ) )
					.isInstanceOf( ConstraintViolationException.class );
		} );
	}

	/**
	 * Merge does not validate by default (empty groups).
	 * Also disables update validation so the dirty merged entity
	 * does not fail at flush time -- we are only testing merge here.
	 */
	@ServiceRegistry(settings = {
			@Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "callback"),
			@Setting(name = ValidationSettings.JAKARTA_INSERT_VALIDATION_GROUP, value = ""),
			@Setting(name = ValidationSettings.JAKARTA_UPDATE_VALIDATION_GROUP, value = ""),
			@Setting(name = BeanValidationIntegrator.APPLY_CONSTRAINTS, value = "false"),
	})
	@DomainModel(annotatedClasses = ValidatedItem.class)
	@SessionFactory
	@Test
	void testMergeNoValidationByDefault(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			var item = new ValidatedItem();
			item.setName( "valid" );
			s.persist( item );
			s.flush();

			item.setName( null );
			s.detach( item );

			// merge should succeed -- no merge validation by default
			s.merge( item );
		} );
	}

	/**
	 * Remove validation fires when pre-remove group is configured.
	 */
	@ServiceRegistry(settings = {
			@Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "callback"),
			@Setting(name = ValidationSettings.JAKARTA_REMOVE_VALIDATION_GROUP,
					value = "jakarta.validation.groups.Default"),
			@Setting(name = BeanValidationIntegrator.APPLY_CONSTRAINTS, value = "false"),
	})
	@DomainModel(annotatedClasses = ValidatedItem.class)
	@SessionFactory
	@Test
	void testRemoveGroupValidation(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			var item = new ValidatedItem();
			item.setName( "valid" );
			s.persist( item );
			s.flush();

			item.setName( null );

			assertThatThrownBy( () -> {
				s.remove( item );
				s.flush();
			} ).isInstanceOf( ConstraintViolationException.class );
		} );
	}

	/**
	 * Both persist and insert validation fire on persist+flush,
	 * each with its own group. The first to fail (persist) throws
	 * with its own violation.
	 */
	@ServiceRegistry(settings = {
			@Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "callback"),
			@Setting(name = ValidationSettings.JAKARTA_PERSIST_VALIDATION_GROUP,
					value = "org.hibernate.orm.test.annotations.beanvalidation.BeanValidationNewLifecycleTest$PersistGroup"),
			@Setting(name = ValidationSettings.JAKARTA_INSERT_VALIDATION_GROUP,
					value = "org.hibernate.orm.test.annotations.beanvalidation.BeanValidationNewLifecycleTest$InsertGroup"),
			@Setting(name = ValidationSettings.JAKARTA_UPDATE_VALIDATION_GROUP, value = ""),
			@Setting(name = BeanValidationIntegrator.APPLY_CONSTRAINTS, value = "false"),
	})
	@DomainModel(annotatedClasses = DualValidatedItem.class)
	@SessionFactory
	@Test
	void testPersistAndInsertBothValidate(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			var item = new DualValidatedItem();
			// both persistField and insertField are null -- violates both groups
			assertThatThrownBy( () -> {
				s.persist( item );
				s.flush();
			} ).isInstanceOf( ConstraintViolationException.class )
					.satisfies( ex -> {
						var violations = ((ConstraintViolationException) ex).getConstraintViolations();
						assertThat( violations )
								.extracting( v -> ((ConstraintViolation<?>) v).getPropertyPath().toString() )
								.containsExactly( "persistField" );
					} );
		} );
	}

	/**
	 * When only persist validation fails but insert would pass,
	 * the persist violation is thrown first.
	 */
	@ServiceRegistry(settings = {
			@Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "callback"),
			@Setting(name = ValidationSettings.JAKARTA_PERSIST_VALIDATION_GROUP,
					value = "org.hibernate.orm.test.annotations.beanvalidation.BeanValidationNewLifecycleTest$PersistGroup"),
			@Setting(name = ValidationSettings.JAKARTA_INSERT_VALIDATION_GROUP,
					value = "org.hibernate.orm.test.annotations.beanvalidation.BeanValidationNewLifecycleTest$InsertGroup"),
			@Setting(name = ValidationSettings.JAKARTA_UPDATE_VALIDATION_GROUP, value = ""),
			@Setting(name = BeanValidationIntegrator.APPLY_CONSTRAINTS, value = "false"),
	})
	@DomainModel(annotatedClasses = DualValidatedItem.class)
	@SessionFactory
	@Test
	void testPersistFailsBeforeInsert(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			var item = new DualValidatedItem();
			item.setInsertField( "valid" ); // insert group passes
			// persistField is null -- persist group fails first
			assertThatThrownBy( () -> {
				s.persist( item );
				s.flush();
			} ).isInstanceOf( ConstraintViolationException.class )
					.satisfies( ex -> {
						var violations = ((ConstraintViolationException) ex).getConstraintViolations();
						assertThat( violations )
								.extracting( v -> ((ConstraintViolation<?>) v).getPropertyPath().toString() )
								.containsExactly( "persistField" );
					} );
		} );
	}

	/**
	 * When persist passes but insert fails, the insert violation is thrown.
	 */
	@ServiceRegistry(settings = {
			@Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "callback"),
			@Setting(name = ValidationSettings.JAKARTA_PERSIST_VALIDATION_GROUP,
					value = "org.hibernate.orm.test.annotations.beanvalidation.BeanValidationNewLifecycleTest$PersistGroup"),
			@Setting(name = ValidationSettings.JAKARTA_INSERT_VALIDATION_GROUP,
					value = "org.hibernate.orm.test.annotations.beanvalidation.BeanValidationNewLifecycleTest$InsertGroup"),
			@Setting(name = ValidationSettings.JAKARTA_UPDATE_VALIDATION_GROUP, value = ""),
			@Setting(name = BeanValidationIntegrator.APPLY_CONSTRAINTS, value = "false"),
	})
	@DomainModel(annotatedClasses = DualValidatedItem.class)
	@SessionFactory
	@Test
	void testInsertFailsWhenPersistPasses(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			var item = new DualValidatedItem();
			item.setPersistField( "valid" ); // persist group passes
			// insertField is null -- insert group fails
			assertThatThrownBy( () -> {
				s.persist( item );
				s.flush();
			} ).isInstanceOf( ConstraintViolationException.class )
					.satisfies( ex -> {
						var violations = ((ConstraintViolationException) ex).getConstraintViolations();
						assertThat( violations )
								.extracting( v -> ((ConstraintViolation<?>) v).getPropertyPath().toString() )
								.containsExactly( "insertField" );
					} );
		} );
	}

	/**
	 * Both remove and delete validation fire on remove+flush,
	 * each with its own group. Remove fires first.
	 */
	@ServiceRegistry(settings = {
			@Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "callback"),
			@Setting(name = ValidationSettings.JAKARTA_INSERT_VALIDATION_GROUP, value = ""),
			@Setting(name = ValidationSettings.JAKARTA_UPDATE_VALIDATION_GROUP, value = ""),
			@Setting(name = ValidationSettings.JAKARTA_REMOVE_VALIDATION_GROUP,
					value = "org.hibernate.orm.test.annotations.beanvalidation.BeanValidationNewLifecycleTest$RemoveGroup"),
			@Setting(name = ValidationSettings.JAKARTA_DELETE_VALIDATION_GROUP,
					value = "org.hibernate.orm.test.annotations.beanvalidation.BeanValidationNewLifecycleTest$DeleteGroup"),
			@Setting(name = BeanValidationIntegrator.APPLY_CONSTRAINTS, value = "false"),
	})
	@DomainModel(annotatedClasses = DualValidatedItem.class)
	@SessionFactory
	@Test
	void testRemoveFailsBeforeDelete(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			var item = new DualValidatedItem();
			item.setRemoveField( "ab" );  // too short, violates @Size(min=3) on RemoveGroup
			item.setDeleteField( "ab" );  // too short, violates @Size(min=3) on DeleteGroup
			s.persist( item );
			s.flush();

			// remove group fires first
			assertThatThrownBy( () -> {
				s.remove( item );
				s.flush();
			} ).isInstanceOf( ConstraintViolationException.class )
					.satisfies( ex -> {
						var violations = ((ConstraintViolationException) ex).getConstraintViolations();
						assertThat( violations )
								.extracting( v -> ((ConstraintViolation<?>) v).getPropertyPath().toString() )
								.containsExactly( "removeField" );
					} );
		} );
	}

	/**
	 * When remove passes but delete fails, the delete violation is thrown.
	 */
	@ServiceRegistry(settings = {
			@Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "callback"),
			@Setting(name = ValidationSettings.JAKARTA_INSERT_VALIDATION_GROUP, value = ""),
			@Setting(name = ValidationSettings.JAKARTA_UPDATE_VALIDATION_GROUP, value = ""),
			@Setting(name = ValidationSettings.JAKARTA_REMOVE_VALIDATION_GROUP,
					value = "org.hibernate.orm.test.annotations.beanvalidation.BeanValidationNewLifecycleTest$RemoveGroup"),
			@Setting(name = ValidationSettings.JAKARTA_DELETE_VALIDATION_GROUP,
					value = "org.hibernate.orm.test.annotations.beanvalidation.BeanValidationNewLifecycleTest$DeleteGroup"),
			@Setting(name = BeanValidationIntegrator.APPLY_CONSTRAINTS, value = "false"),
	})
	@DomainModel(annotatedClasses = DualValidatedItem.class)
	@SessionFactory
	@Test
	void testDeleteFailsWhenRemovePasses(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			var item = new DualValidatedItem();
			item.setRemoveField( "valid" ); // long enough, remove group passes
			item.setDeleteField( "ab" );    // too short, delete group fails
			s.persist( item );
			s.flush();

			assertThatThrownBy( () -> {
				s.remove( item );
				s.flush();
			} ).isInstanceOf( ConstraintViolationException.class )
					.satisfies( ex -> {
						var violations = ((ConstraintViolationException) ex).getConstraintViolations();
						assertThat( violations )
								.extracting( v -> ((ConstraintViolation<?>) v).getPropertyPath().toString() )
								.containsExactly( "deleteField" );
					} );
		} );
	}
}
