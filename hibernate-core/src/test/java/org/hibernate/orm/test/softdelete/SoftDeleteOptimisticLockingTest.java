package org.hibernate.orm.test.softdelete;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OptimisticLockException;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.annotations.OptimisticLockType.ALL;
import static org.hibernate.annotations.OptimisticLockType.DIRTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JiraKey("HHH-21263")
@DomainModel(annotatedClasses = {
		SoftDeleteOptimisticLockingTest.AllItem.class,
		SoftDeleteOptimisticLockingTest.DirtyItem.class
})
@SessionFactory
@ParameterizedClass
@ValueSource(classes = { SoftDeleteOptimisticLockingTest.AllItem.class, SoftDeleteOptimisticLockingTest.DirtyItem.class })
class SoftDeleteOptimisticLockingTest {
	@Parameter
	Class<? extends Item> itemType;

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.dropData();
	}

	private void persistItem(String name, SessionFactoryScope scope) {
		final Item item = itemType == AllItem.class ? new AllItem() : new DirtyItem();
		item.id = 1;
		item.name = name;
		scope.inTransaction( session -> session.persist( item ) );
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = "original")
	void rejectsStaleDelete(String originalName, SessionFactoryScope scope) {
		persistItem( originalName, scope );
		scope.inSession( session -> {
			final var item = scope.fromTransaction( session, s -> s.find( itemType, 1 ) );
			scope.inTransaction( other -> other.find( itemType, 1 ).name = "changed" );
			assertThrows( OptimisticLockException.class, () -> scope.inTransaction( session, s -> {
				s.remove( item );
				s.flush();
			} ) );
		} );
		scope.inTransaction( session -> assertEquals( "changed", session.find( itemType, 1 ).name ) );
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = "original")
	void ignoresChangesToExcludedAttribute(String originalName, SessionFactoryScope scope) {
		persistItem( originalName, scope );
		scope.inSession( session -> {
			final var item = scope.fromTransaction( session, s -> s.find( itemType, 1 ) );
			scope.inTransaction( other -> other.find( itemType, 1 ).note = "changed" );
			scope.inTransaction( session, s -> s.remove( item ) );
		} );
		scope.inTransaction( session -> {
			assertNull( session.find( itemType, 1 ) );
			assertEquals( "changed", session.createNativeQuery(
					"select note from " + itemType.getSimpleName() + " where id = 1", String.class ).getSingleResult() );
		} );
	}

	@Test
	void removesReadOnlyInstance(SessionFactoryScope scope) {
		persistItem( "original", scope );
		scope.inTransaction( session -> {
			session.setDefaultReadOnly( true );
			final var item = session.find( itemType, 1 );
			item.name = "in memory";
			session.remove( item );
		} );
		scope.inTransaction( session -> assertNull( session.find( itemType, 1 ) ) );
	}

	@MappedSuperclass
	static class Item {
		@Id
		Integer id;
		String name;
		@OptimisticLock(excluded = true)
		String note;
	}

	@Entity(name = "AllItem")
	@SoftDelete
	@OptimisticLocking(type = ALL)
	@DynamicUpdate
	static class AllItem extends Item {
	}

	@Entity(name = "DirtyItem")
	@SoftDelete
	@OptimisticLocking(type = DIRTY)
	@DynamicUpdate
	static class DirtyItem extends Item {
	}
}
