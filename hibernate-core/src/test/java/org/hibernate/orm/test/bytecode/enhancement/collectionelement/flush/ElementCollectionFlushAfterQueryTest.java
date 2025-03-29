/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.collectionelement.flush;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;


@DomainModel(
		annotatedClasses = {
				ElementCollectionFlushAfterQueryTest.MyEntity.class,
				ElementCollectionFlushAfterQueryTest.MyOtherEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
@JiraKey("HHH-16337")
public class ElementCollectionFlushAfterQueryTest {
	private static final Long MY_ENTITY_ID = 1l;

	@Test
	public void testAutoFlush(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					MyEntity myEntity = new MyEntity( MY_ENTITY_ID, "my entity" );
					myEntity.addRedirectUris( "1" );
					session.persist( myEntity );
				}
		);

		scope.inTransaction(
				session -> {
					MyEntity myEntity = session.find( MyEntity.class, MY_ENTITY_ID );

					Set<String> set = new HashSet<>();
					set.add( "3" );
					myEntity.setRedirectUris( set );

					session.createQuery( "from MyOtherEntity ", MyOtherEntity.class ).getResultList();
				}
		);

		scope.inTransaction(
				session -> {
					MyEntity myEntity = session.find( MyEntity.class, MY_ENTITY_ID );
					Set<String> redirectUris = myEntity.getRedirectUris();
					assertThat( redirectUris.size() ).isEqualTo( 1 );
					assertThat( redirectUris.contains( "3" ) );
				}
		);

	}

	@Entity(name = "MyEntity")
	public static class MyEntity {
		@Id
		private Long id;

		private String name;

		@ElementCollection
		@Column(name = "val")
		@CollectionTable(name = "REDIRECT_URIS", joinColumns = { @JoinColumn(name = "ID") })
		protected Set<String> redirectUris = new HashSet<>();

		public MyEntity() {
		}

		public MyEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return this.id;
		}

		public Set<String> getRedirectUris() {
			return redirectUris;
		}

		public void setRedirectUris(Set<String> redirectUris) {
			this.redirectUris = redirectUris;
		}

		public void addRedirectUris(String redirectUri) {
			this.redirectUris.add( redirectUri );
		}
	}

	@Entity(name = "MyOtherEntity")
	public class MyOtherEntity {

		@Id
		private Long id;

		private String name;

		public MyOtherEntity() {
		}

		public MyOtherEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return this.id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
