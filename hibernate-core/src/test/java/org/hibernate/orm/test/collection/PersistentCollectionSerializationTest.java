/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.collection.spi.PersistentList;
import org.hibernate.collection.spi.PersistentMap;
import org.hibernate.collection.spi.PersistentSet;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderColumn;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = PersistentCollectionSerializationTest.Owner.class)
@SessionFactory
public class PersistentCollectionSerializationTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Owner owner = new Owner();
			owner.id = 1L;
			owner.names = new HashSet<>( Set.of( "a", "b", "c" ) );
			owner.items = new ArrayList<>( List.of( "first", "second", "third" ) );
			owner.labels = new HashMap<>( Map.of( "k1", "v1", "k2", "v2" ) );
			session.persist( owner );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testPersistentSetSerialization(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Owner owner = session.find( Owner.class, 1L );
			Set<String> set = owner.names;
			assertThat( set ).isInstanceOf( PersistentSet.class );

			@SuppressWarnings("unchecked")
			Set<String> cloned = (Set<String>) SerializationHelper.clone( (Serializable) set );

			assertThat( cloned ).containsExactlyInAnyOrderElementsOf( set );
		} );
	}

	@Test
	public void testPersistentListSerialization(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Owner owner = session.find( Owner.class, 1L );
			List<String> list = owner.items;
			assertThat( list ).isInstanceOf( PersistentList.class );

			@SuppressWarnings("unchecked")
			List<String> cloned = (List<String>) SerializationHelper.clone( (Serializable) list );

			assertThat( cloned ).containsExactlyElementsOf( list );
		} );
	}

	@Test
	public void testPersistentMapSerialization(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Owner owner = session.find( Owner.class, 1L );
			Map<String, String> map = owner.labels;
			assertThat( map ).isInstanceOf( PersistentMap.class );

			@SuppressWarnings("unchecked")
			Map<String, String> cloned = (Map<String, String>) SerializationHelper.clone( (Serializable) map );

			assertThat( cloned ).containsExactlyInAnyOrderEntriesOf( map );
		} );
	}

	@Entity(name = "SerOwner")
	public static class Owner implements Serializable {
		@Id
		Long id;

		@ElementCollection(fetch = FetchType.EAGER)
		Set<String> names;

		@ElementCollection(fetch = FetchType.EAGER)
		@OrderColumn
		List<String> items;

		@ElementCollection(fetch = FetchType.EAGER)
		@MapKeyColumn(name = "label_key")
		Map<String, String> labels;
	}
}
