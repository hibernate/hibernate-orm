/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version;

import java.util.List;
import java.util.UUID;

import org.hibernate.PropertyValueException;
import org.hibernate.exception.ConstraintViolationException;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				DetachedEntityWithNullVersionTest.IdentityGeneratedIdItem.class,
				DetachedEntityWithNullVersionTest.SequenceGeneratedIdItem.class,
				DetachedEntityWithNullVersionTest.TableGeneratedIdItem.class,
				DetachedEntityWithNullVersionTest.AssignedIdItem.class,
				DetachedEntityWithNullVersionTest.UUIDIdItem.class,
		}
)
@SessionFactory
@JiraKey("HHH-16586")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
public class DetachedEntityWithNullVersionTest {

	private static final String ITEM_INITIAL_NAME = "initial name";
	private static final String ITEM_UPDATED_NAME = "updated name";

	@Test
	public void testMergeDetachedEntityWithIdentityId(SessionFactoryScope scope) {
		IdentityGeneratedIdItem item = new IdentityGeneratedIdItem();
		persistItem( scope, item );

		// The Item we are going to merge has the id but version null,
		// for generated id Hibernate can detect that the instance is not transient
		// so because of the discrepancy of the null version
		// that implies a transient instance an exception should be thrown
		Assertions.assertThrows( PropertyValueException.class, () ->
				scope.inTransaction(
						session -> {
							IdentityGeneratedIdItem item1 = new IdentityGeneratedIdItem();
							item1.id = item.getId();
							item1.setName( ITEM_UPDATED_NAME );
							session.merge( item1 );
						}
				)
		);

		assertItemHasNotBeenUpdated( item, scope );

		updateItem( item, scope );

		assertItemHasBeenUpdated( item, scope );
	}

	@Test
	public void testMergeDetachedEntityWithSequenceId(SessionFactoryScope scope) {
		SequenceGeneratedIdItem item = new SequenceGeneratedIdItem();
		persistItem( scope, item );

		// The Item we are going to merge has the id but version null,
		// for generated id Hibernate can detect that the instance is not transient
		// so because of the discrepancy of the null version
		// that implies a transient instance an exception should be thrown
		Assertions.assertThrows( PropertyValueException.class, () ->
				scope.inTransaction(
						session -> {
							SequenceGeneratedIdItem item1 = new SequenceGeneratedIdItem();
							item1.id = item.getId();
							item1.setName( ITEM_UPDATED_NAME );
							session.merge( item1 );
						}
				)
		);

		assertItemHasNotBeenUpdated( item, scope );

		updateItem( item, scope );

		assertItemHasBeenUpdated( item, scope );
	}

	@Test
	public void testMergeDetachedEntityWithTableGerneratedId(SessionFactoryScope scope) {
		TableGeneratedIdItem item = new TableGeneratedIdItem();
		persistItem( scope, item );

		// The Item we are going to merge has the id but version null,
		// for generated id Hibernate can detect that the instance is not transient
		// so because of the discrepancy of the null version
		// that implies a transient instance an exception should be thrown
		Assertions.assertThrows( PropertyValueException.class, () ->
				scope.inTransaction(
						session -> {
							TableGeneratedIdItem item1 = new TableGeneratedIdItem();
							item1.id = item.getId();
							item1.setName( ITEM_UPDATED_NAME );
							session.merge( item1 );
						}
				)
		);

		assertItemHasNotBeenUpdated( item, scope );

		updateItem( item, scope );

		assertItemHasBeenUpdated( item, scope );
	}

	@Test
	public void testMergeDetachedEntityWithUUIDId(SessionFactoryScope scope) {
		UUIDIdItem item = new UUIDIdItem();
		persistItem( scope, item );

		// The Item we are going to merge has the id but version null,
		// for generated id Hibernate can detect that the instance is not transient
		// so because of the discrepancy of the null version
		// that implies a transient instance an exception should be thrownAssertions.assertThrows( HibernateException.class, () ->
		Assertions.assertThrows( PropertyValueException.class, () ->
				scope.inTransaction(
						session -> {
							UUIDIdItem item1 = new UUIDIdItem();
							item1.id = item.getId();
							item1.setName( ITEM_UPDATED_NAME );
							session.merge( item1 );
						}
				)
		);

		assertItemHasNotBeenUpdated( item, scope );

		updateItem( item, scope );

		assertItemHasBeenUpdated( item, scope );
	}

	@Test
	public void testMergeDetachedEntityWithAssignedId(SessionFactoryScope scope) {
		AssignedIdItem item = new AssignedIdItem( 1l, ITEM_INITIAL_NAME );
		persistItem( scope, item );

		// we are not setting the version but the new Item has the id of a persisted entity
		// Hibernate for assigned id does not detect that this is a detached instance so consider it
		// a transient instance because of the null version
		// so a constraint exception is thrown when Hibernate tries to persist it
		Assertions.assertThrows( ConstraintViolationException.class, () ->
				scope.inTransaction(
						session -> {
							AssignedIdItem item1 = new AssignedIdItem( 1l, ITEM_UPDATED_NAME );
							session.merge( item1 );
						}
				)
		);

		assertItemHasNotBeenUpdated( item, scope );

		updateItem( item, scope );

		assertItemHasBeenUpdated( item, scope );
	}

	private static void persistItem(SessionFactoryScope scope, ItemInterface item) {
		scope.inTransaction(
				session -> {
					item.setName( ITEM_INITIAL_NAME );
					session.persist( item );
				}
		);
	}

	private static void assertItemHasNotBeenUpdated(ItemInterface item, SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<ItemInterface> items = session.createQuery( "select it from " + item.getClass()
							.getName() + " it" ).list();
					assertThat( items.size() ).isEqualTo( 1 );
					assertThat( items.get( 0 ).getName() ).isEqualTo( ITEM_INITIAL_NAME );
				}
		);
	}

	private static void updateItem(ItemInterface item, SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					item.setName( ITEM_UPDATED_NAME );
					session.merge( item );
				}
		);
	}

	private static void assertItemHasBeenUpdated(ItemInterface item, SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<ItemInterface> items = session.createQuery( "select it from " + item.getClass()
									.getName() + " it" )
							.list();
					assertThat( items.size() ).isEqualTo( 1 );
					assertThat( items.get( 0 ).getName() ).isEqualTo( ITEM_UPDATED_NAME );
				}
		);
	}

	public interface ItemInterface {
		Long getVersion();

		void setVersion(Long version);

		String getName();

		void setName(String name);
	}

	@Entity(name = "IdentityGeneratedIdItem")
	@Table(name = "ITEM_TABLE")
	public static class IdentityGeneratedIdItem implements ItemInterface {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@Version
		private Long version;

		private String name;

		public Long getId() {
			return id;
		}

		@Override
		public Long getVersion() {
			return version;
		}

		@Override
		public void setVersion(Long version) {
			this.version = version;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "SequenceGeneratedIdItem")
	@Table(name = "ITEM_TABLE_2")
	public static class SequenceGeneratedIdItem implements ItemInterface {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)

		private Long id;

		@Version
		private Long version;

		private String name;

		public Long getId() {
			return id;
		}

		@Override
		public Long getVersion() {
			return version;
		}

		@Override
		public void setVersion(Long version) {
			this.version = version;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "TableGeneratedIdItem")
	@Table(name = "ITEM_TABLE_3")
	public static class TableGeneratedIdItem implements ItemInterface {

		@Id
		@GeneratedValue(strategy = GenerationType.TABLE)
		private Long id;

		@Version
		private Long version;

		private String name;

		public Long getId() {
			return id;
		}

		@Override
		public Long getVersion() {
			return version;
		}

		@Override
		public void setVersion(Long version) {
			this.version = version;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "AssignedIdItem")
	@Table(name = "ITEM_TABLE_4")
	public static class AssignedIdItem implements ItemInterface {

		@Id
		private Long id;

		@Version
		private Long version;

		private String name;

		public AssignedIdItem() {
		}

		public AssignedIdItem(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		@Override
		public Long getVersion() {
			return version;
		}

		@Override
		public void setVersion(Long version) {
			this.version = version;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "UUIDIdItem")
	@Table(name = "ITEM_TABLE_5")
	public static class UUIDIdItem implements ItemInterface {

		@Id
		@GeneratedValue(strategy = GenerationType.UUID)
		private UUID id;

		@Version
		private Long version;

		private String name;

		public UUIDIdItem() {
		}

		public UUIDIdItem(String name) {
			this.name = name;
		}

		public UUID getId() {
			return id;
		}

		@Override
		public Long getVersion() {
			return version;
		}

		@Override
		public void setVersion(Long version) {
			this.version = version;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}
	}
}
