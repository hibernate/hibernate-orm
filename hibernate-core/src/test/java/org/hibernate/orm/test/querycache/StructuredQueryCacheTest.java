/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalceca
 */
@JiraKey( value = "HHH-12107" )
@DomainModel(
		annotatedClasses = {
				StructuredQueryCacheTest.OneToManyWithEmbeddedId.class,
				StructuredQueryCacheTest.OneToManyWithEmbeddedIdChild.class,
				StructuredQueryCacheTest.OneToManyWithEmbeddedIdKey.class
		},
		concurrencyStrategy = "transactional"
)
@SessionFactory
@ServiceRegistry(settings = {
		@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "true" ),
		@Setting( name = AvailableSettings.CACHE_REGION_PREFIX, value = "foo" ),
		@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ),
		@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
		@Setting( name = AvailableSettings.USE_STRUCTURED_CACHE, value = "true" ),
})
public class StructuredQueryCacheTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey( value = "HHH-12107" )
	public void testEmbeddedIdInOneToMany(SessionFactoryScope scope) {

		OneToManyWithEmbeddedIdKey key = new OneToManyWithEmbeddedIdKey( 1234 );
		final OneToManyWithEmbeddedId o = new OneToManyWithEmbeddedId( key );
		o.setItems( new HashSet<>() );
		o.getItems().add( new OneToManyWithEmbeddedIdChild( 1 ) );

		scope.inTransaction( session ->
			session.persist( o )
		);

		scope.inTransaction( session -> {
			OneToManyWithEmbeddedId _entity = session.find( OneToManyWithEmbeddedId.class, key );
			assertTrue( session.getSessionFactory().getCache().containsEntity( OneToManyWithEmbeddedId.class, key ) );
			assertNotNull( _entity );
		});

		scope.inTransaction( session -> {
			OneToManyWithEmbeddedId _entity = session.find( OneToManyWithEmbeddedId.class, key );
			assertTrue( session.getSessionFactory().getCache().containsEntity( OneToManyWithEmbeddedId.class, key ) );
			assertNotNull( _entity );
		});
	}

	@Entity(name = "OneToManyWithEmbeddedId")
	public static class OneToManyWithEmbeddedId {

		private OneToManyWithEmbeddedIdKey id;

		private String name;

		private Set<OneToManyWithEmbeddedIdChild> items = new HashSet<>(  );

		public OneToManyWithEmbeddedId() {
		}

		public OneToManyWithEmbeddedId(OneToManyWithEmbeddedIdKey id) {
			this.id = id;
		}

		@EmbeddedId
		public OneToManyWithEmbeddedIdKey getId() {
			return id;
		}

		public void setId(OneToManyWithEmbeddedIdKey id) {
			this.id = id;
		}

		@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, targetEntity = OneToManyWithEmbeddedIdChild.class, orphanRemoval = true)
		@JoinColumn(name = "parent_id")
		public Set<OneToManyWithEmbeddedIdChild> getItems() {
			return items;
		}

		public void setItems(Set<OneToManyWithEmbeddedIdChild> items) {
			this.items = items;
		}
	}

	@Entity(name = "OneToManyWithEmbeddedIdChild")
	public static class OneToManyWithEmbeddedIdChild {
		private Integer id;

		public String name;

		public OneToManyWithEmbeddedIdChild() {
		}

		public OneToManyWithEmbeddedIdChild(Integer id) {
			this.id = id;
		}

		@Id
		@Column(name = "id")
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class OneToManyWithEmbeddedIdKey implements Serializable {
		private Integer id;

		public OneToManyWithEmbeddedIdKey() {
		}

		public OneToManyWithEmbeddedIdKey(Integer id) {
			this.id = id;
		}

		@Column(name = "id")
		public Integer getId() {
			return this.id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}
}
