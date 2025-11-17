/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gail Badner
 */
@JiraKey(value = "HHH-11881")
@DomainModel(
		annotatedClasses = {
				SetElementNullBasicTest.AnEntity.class

		}
)
@SessionFactory
public class SetElementNullBasicTest {

	@Test
	public void testPersistNullValue(SessionFactoryScope scope) {
		int entityId = scope.fromTransaction( session -> {
					AnEntity e = new AnEntity();
					e.aCollection.add( null );
					session.persist( e );
					return e.id;
				}
		);

		scope.inTransaction( session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertThat( e.aCollection.size() ).isEqualTo( 0 );
					assertThat( getCollectionElementRows( entityId, scope ) ).hasSize( 0 );
					session.remove( e );
				}
		);
	}

	@Test
	public void addNullValue(SessionFactoryScope scope) {
		int entityId = scope.fromTransaction( session -> {
					AnEntity e = new AnEntity();
					session.persist( e );
					return e.id;
				}
		);

		scope.inTransaction( session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertThat( e.aCollection.size() ).isEqualTo( 0 );
					assertThat( getCollectionElementRows( entityId, scope ) ).hasSize( 0 );
					e.aCollection.add( null );
				}
		);

		scope.inTransaction( session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertThat( e.aCollection.size() ).isEqualTo( 0 );
					assertThat( getCollectionElementRows( entityId, scope ) ).hasSize( 0 );
					session.remove( e );
				}
		);
	}

	@Test
	public void testUpdateNonNullValueToNull(SessionFactoryScope scope) {
		int entityId = scope.fromTransaction( session -> {
					AnEntity e = new AnEntity();
					e.aCollection.add( "def" );
					session.persist( e );
					return e.id;
				}
		);

		scope.inTransaction( session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertThat( e.aCollection.size() ).isEqualTo( 1 );
					assertThat( getCollectionElementRows( entityId, scope ) ).hasSize( 1 );
					e.aCollection.remove( "def" );
					e.aCollection.add( null );
				}
		);

		scope.inTransaction( session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertThat( e.aCollection.size() ).isEqualTo( 0 );
					assertThat( getCollectionElementRows( entityId, scope ) ).hasSize( 0 );
					session.remove( e );
				}
		);
	}

	private List<?> getCollectionElementRows(int id, SessionFactoryScope scope) {
		return scope.fromSession( session ->
				session.createNativeQuery(
						"SELECT aCollection FROM AnEntity_aCollection where AnEntity_id = " + id
				).list()
		);
	}

	@Entity(name = "AnEntity")
	@Table(name = "AnEntity")
	public static class AnEntity {
		@Id
		@GeneratedValue
		private int id;

		@ElementCollection
		@CollectionTable(name = "AnEntity_aCollection", joinColumns = {@JoinColumn(name = "AnEntity_id")})
		private Set<String> aCollection = new HashSet<>();
	}
}
