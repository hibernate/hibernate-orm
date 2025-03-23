/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		CollectionCacheEmbeddedIdKeyTest.CatalogValueId.class,
		CollectionCacheEmbeddedIdKeyTest.MetadataValue.class,
		CollectionCacheEmbeddedIdKeyTest.CatalogValue.class,
} )
@ServiceRegistry( settings = {
		@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ),
		@Setting( name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "30" ),
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16845" )
public class CollectionCacheEmbeddedIdKeyTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CatalogValue value1 = new CatalogValue( new CatalogValueId( "id_1" ), "key_1" );
			value1.getMetadata().put( "metadata_1", new MetadataValue( "data_1", true ) );
			session.persist( value1 );
			final CatalogValue value2 = new CatalogValue( new CatalogValueId( "id_2" ), "key_2" );
			value2.getMetadata().put( "metadata_2", new MetadataValue( "data_2", false ) );
			session.persist( value2 );
			session.persist( new CatalogValue( new CatalogValueId( "id_3" ), "key_3" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from CatalogValue" ).executeUpdate() );
	}

	@Test
	public void testFindWithMetadata(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictAllRegions();
		scope.inTransaction( session -> {
			final CatalogValue catalogValue = findByKey( session, "key_1" );
			assertThat( Hibernate.isInitialized( catalogValue.getMetadata() ) ).isFalse();
			findByKey( session, "key_2" );
			catalogValue.getMetadata().size(); // trigger collection initialization
			assertThat( Hibernate.isInitialized( catalogValue.getMetadata() ) ).isTrue();
		} );
	}

	@Test
	public void testFindWithoutMetadata(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictAllRegions();
		scope.inTransaction( session -> {
			final CatalogValue catalogValue = findByKey( session, "key_1" );
			assertThat( Hibernate.isInitialized( catalogValue.getMetadata() ) ).isFalse();
			findByKey( session, "key_3" );
			catalogValue.getMetadata().size(); // trigger collection initialization
			assertThat( Hibernate.isInitialized( catalogValue.getMetadata() ) ).isTrue();
		} );
	}

	private CatalogValue findByKey(Session session, String valueKey) {
		final CriteriaBuilder cb = session.getCriteriaBuilder();
		final CriteriaQuery<CatalogValue> query = cb.createQuery( CatalogValue.class );
		final Root<CatalogValue> root = query.from( CatalogValue.class );
		query.where( cb.equal( root.get( "valueKey" ), valueKey ) );
		final List<CatalogValue> result = session.createQuery( query ).getResultList();
		if ( result.isEmpty() ) {
			throw new IllegalStateException( "mandatory catalog value not found " + valueKey );
		}
		if ( result.size() == 1 ) {
			return result.get( 0 );
		}
		throw new IllegalStateException( "multiple entities foun for the same key " + valueKey );
	}

	@Embeddable
	public static class CatalogValueId implements Serializable {
		private String id;

		public CatalogValueId() {
		}

		public CatalogValueId(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}
	}

	@Embeddable
	public static class MetadataValue {
		private String data;

		private boolean isPublic;

		public MetadataValue() {
		}

		public MetadataValue(String data, boolean isPublic) {
			this.data = data;
			this.isPublic = isPublic;
		}
	}

	@Entity( name = "CatalogValue" )
	@Cacheable
	@Cache( usage = CacheConcurrencyStrategy.READ_ONLY )
	public static class CatalogValue {
		@EmbeddedId
		private CatalogValueId valueId;

		private String valueKey;

		@ElementCollection
		@CollectionTable( name = "catalog_value_metadata", joinColumns = {
				@JoinColumn( name = "catalog_value_id", referencedColumnName = "id" )
		} )
		@Cache( usage = CacheConcurrencyStrategy.READ_ONLY )
		private Map<String, MetadataValue> metadata = new HashMap<>();

		public CatalogValue() {
		}

		public CatalogValue(CatalogValueId valueId, String valueKey) {
			this.valueId = valueId;
			this.valueKey = valueKey;
		}

		public Map<String, MetadataValue> getMetadata() {
			return metadata;
		}
	}
}
