/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.readonly;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceException;
import org.hibernate.ReadOnlyMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses = {ReadOnlyCollectionTest.ModifiableEntity.class})
class ReadOnlyCollectionTest {
	@Test void test(SessionFactoryScope scope) {
		UUID uuid = scope.fromTransaction( session -> {
			var entity = new ModifiableEntity();
			entity.strings.add( "foo" );
			session.persist( entity );
			return entity.uuid;
		} );
		assertThrows( PersistenceException.class, () -> scope.inTransaction( session -> {
			var entity =
					session.find( ModifiableEntity.class, uuid,
							ReadOnlyMode.READ_ONLY );
			assertTrue( session.isReadOnly( entity ) );
			assertEquals( List.of( "foo" ), entity.strings );
			entity.strings.add( "bar" );
		} ) );
		assertThrows( PersistenceException.class, () -> scope.inTransaction( session -> {
			var entity =
					session.find( ModifiableEntity.class, uuid,
							ReadOnlyMode.READ_ONLY );
			assertEquals( List.of( "foo" ), entity.strings );
			entity.strings.clear();
		} ) );
		assertThrows( PersistenceException.class, () ->
				scope.inTransaction( session -> {
			var entity =
					session.find( ModifiableEntity.class, uuid,
							ReadOnlyMode.READ_ONLY );
			assertEquals( List.of( "foo" ), entity.strings );
			entity.strings = null;
		} ) );
		scope.inTransaction( session -> {
			var entity =
					session.find( ModifiableEntity.class, uuid,
							ReadOnlyMode.READ_ONLY );
			assertEquals( List.of( "foo" ), entity.strings );
		} );
	}
	@Entity(name = "ModifiableEntity")
	static class ModifiableEntity {
		@GeneratedValue
		@Id UUID uuid;
		double numericValue;
		@ElementCollection
		@CollectionTable(name = "STRINGS")
		List<String> strings = new ArrayList<>();
	}
}
