package org.hibernate.test.version;

import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PersistenceException;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.PropertyValueException;
import org.hibernate.exception.ConstraintViolationException;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;


@TestForIssue(jiraKey = "HHH-16586")
@RequiresDialectFeature(value = { DialectChecks.SupportsSequences.class, DialectChecks.SupportsIdentityColumns.class })
public class DetachedEntityWithNullVersionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				IdentityGeneratedIdItem.class,
				SequenceGeneratedIdItem.class,
				TableGeneratedIdItem.class,
				AssignedIdItem.class,
				UUIDIdItem.class
		};
	}

	private static final String ITEM_INITIAL_NAME = "initial name";
	private static final String ITEM_UPDATED_NAME = "updated name";

	@Test
	public void testMergeDetachedEntityWithIdentityId() {
		IdentityGeneratedIdItem item = new IdentityGeneratedIdItem();
		persistItem( item );

		try {
			// The Item we are going to merge has the id but version null,
			// for generated id Hibernate can detect that the instance is not transient
			// so because of the discrepancy of the null version
			// that implies a transient instance an exception should be thrown
			inTransaction(
					session -> {
						IdentityGeneratedIdItem item1 = new IdentityGeneratedIdItem();
						item1.id = item.getId();
						item1.setName( ITEM_UPDATED_NAME );
						session.merge( item1 );
					}
			);
			fail( "A PropertyValueException is expected" );
		}
		catch (Exception e) {
			assertThat( e, instanceOf( PersistenceException.class ) );
			assertThat( e.getCause(), instanceOf( PropertyValueException.class ) );
		}

		assertItemHasNotBeenUpdated( item );

		updateItem( item );

		assertItemHasBeenUpdated( item );
	}

	@Test
	public void testMergeDetachedEntityWithSequenceId() {
		SequenceGeneratedIdItem item = new SequenceGeneratedIdItem();
		persistItem( item );

		try {
			// The Item we are going to merge has the id but version null,
			// for generated id Hibernate can detect that the instance is not transient
			// so because of the discrepancy of the null version
			// that implies a transient instance an exception should be thrown
			inTransaction(
					session -> {
						SequenceGeneratedIdItem item1 = new SequenceGeneratedIdItem();
						item1.id = item.getId();
						item1.setName( ITEM_UPDATED_NAME );
						session.merge( item1 );
					}
			);
			fail( "A PropertyValueException is expected" );
		}
		catch (Exception e) {
			assertThat( e, instanceOf( PersistenceException.class ) );
			assertThat( e.getCause(), instanceOf( PropertyValueException.class ) );
		}

		assertItemHasNotBeenUpdated( item );

		updateItem( item );

		assertItemHasBeenUpdated( item );
	}

	@Test
	public void testMergeDetachedEntityWithTableGerneratedId() {
		TableGeneratedIdItem item = new TableGeneratedIdItem();
		persistItem( item );

		try {
			// The Item we are going to merge has the id but version null,
			// for generated id Hibernate can detect that the instance is not transient
			// so because of the discrepancy of the null version
			// that implies a transient instance an exception should be thrown
			inTransaction(
					session -> {
						TableGeneratedIdItem item1 = new TableGeneratedIdItem();
						item1.id = item.getId();
						item1.setName( ITEM_UPDATED_NAME );
						session.merge( item1 );
					}
			);
			fail( "A PropertyValueException is expected" );
		}
		catch (Exception e) {
			assertThat( e, instanceOf( PersistenceException.class ) );
			assertThat( e.getCause(), instanceOf( PropertyValueException.class ) );
		}

		assertItemHasNotBeenUpdated( item );

		updateItem( item );

		assertItemHasBeenUpdated( item );
	}

	@Test
	public void testMergeDetachedEntityWithUUIDId() {
		UUIDIdItem item = new UUIDIdItem();
		persistItem( item );

		try {
			// The Item we are going to merge has the id but version null,
			// for generated id Hibernate can detect that the instance is not transient
			// so because of the discrepancy of the null version
			// that implies a transient instance an exception should be thrownAssertions.assertThrows( HibernateException.class, () ->
			inTransaction(
					session -> {
						UUIDIdItem item1 = new UUIDIdItem();
						item1.id = item.getId();
						item1.setName( ITEM_UPDATED_NAME );
						session.merge( item1 );
					}
			);
			fail( "A PropertyValueException is expected" );
		}
		catch (Exception e) {
			assertThat( e, instanceOf( PersistenceException.class ) );
			assertThat( e.getCause(), instanceOf( PropertyValueException.class ) );
		}

		assertItemHasNotBeenUpdated( item );

		updateItem( item );

		assertItemHasBeenUpdated( item );
	}

	@Test
	public void testMergeDetachedEntityWithAssignedId() {
		AssignedIdItem item = new AssignedIdItem( 1l, ITEM_INITIAL_NAME );
		persistItem( item );

		try {
			// we are not setting the version but the new Item has the id of a persisted entity
			// Hibernate for assigned id does not detect that this is a detached instance so consider it
			// a transient instance because of the null version
			// so a constraint exception is thrown when Hibernate tries to persist it
			inTransaction(
					session -> {
						AssignedIdItem item1 = new AssignedIdItem( 1l, ITEM_UPDATED_NAME );
						session.merge( item1 );
					}
			);
			fail( "A ConstraintViolationException is expected" );
		}
		catch (Exception e) {
			assertThat( e, instanceOf( PersistenceException.class ) );
			assertThat( e.getCause(), instanceOf( ConstraintViolationException.class ) );		}

		assertItemHasNotBeenUpdated( item );

		updateItem( item );

		assertItemHasBeenUpdated( item );
	}

	private void persistItem(ItemInterface item) {
		inTransaction(
				session -> {
					item.setName( ITEM_INITIAL_NAME );
					session.persist( item );
				}
		);
	}

	private void assertItemHasNotBeenUpdated(ItemInterface item) {
		inTransaction(
				session -> {
					List<ItemInterface> items = session.createQuery( "select it from " + item.getClass()
							.getName() + " it" ).list();
					assertThat( items.size(), is( 1 ) );
					assertThat( items.get( 0 ).getName(), is( ITEM_INITIAL_NAME ) );
				}
		);
	}

	private void updateItem(ItemInterface item) {
		inTransaction(
				session -> {
					item.setName( ITEM_UPDATED_NAME );
					session.merge( item );
				}
		);
	}

	private void assertItemHasBeenUpdated(ItemInterface item) {
		inTransaction(
				session -> {
					List<ItemInterface> items = session.createQuery( "select it from " + item.getClass()
									.getName() + " it" )
							.list();
					assertThat( items.size(), is( 1 ) );
					assertThat( items.get( 0 ).getName(), is( ITEM_UPDATED_NAME ) );
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
		@GeneratedValue
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

