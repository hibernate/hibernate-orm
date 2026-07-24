/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A Criteria path traversal through a to-one association whose target uses JOINED inheritance,
 * where the foreign key targets the identifier (declared on the root table), generates SQL with a
 * mismatched table alias in the join ON clause (e.g. {@code join mappings_a ma1_0 on ma1_1.id=...}
 * where {@code ma1_1} is never declared). Both {@code @OneToOne} and {@code @ManyToOne} are affected.
 */
@Jpa(
		annotatedClasses = {
				CriteriaJoinedInheritanceToOneJoinAliasTest.Asset.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.BaseMapping.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.MappingA.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.Request.class,
		},
		useCollectingStatementInspector = true
)
public class CriteriaJoinedInheritanceToOneJoinAliasTest {

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Asset asset = new Asset( "a1", "some-label" );
			entityManager.persist( asset );

			MappingA mapping = new MappingA( "m1", asset );
			entityManager.persist( mapping );

			Request request = new Request( "r1", mapping );
			entityManager.persist( request );
		} );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "delete from Request" ).executeUpdate();
			entityManager.createQuery( "delete from MappingA" ).executeUpdate();
			entityManager.createQuery( "delete from BaseMapping" ).executeUpdate();
			entityManager.createQuery( "delete from Asset" ).executeUpdate();
		} );
	}

	@Test
	public void testImplicitJoinThroughOneToOne(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Request> query = cb.createQuery( Request.class );
			Root<Request> root = query.from( Request.class );

			Path<String> label = root
					.get( "oneToOneMapping" )
					.get( "asset" )
					.get( "label" );
			query.where( cb.like( cb.lower( label ), "%some%" ) );

			List<Request> results = entityManager.createQuery( query ).getResultList();
			assertThat( results ).hasSize( 1 );
			assertThat( results.get( 0 ).getId() ).isEqualTo( "r1" );
		} );
	}

	@Test
	public void testImplicitJoinThroughManyToOne(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Request> query = cb.createQuery( Request.class );
			Root<Request> root = query.from( Request.class );

			Path<String> label = root
					.get( "manyToOneMapping" )
					.get( "asset" )
					.get( "label" );
			query.where( cb.like( cb.lower( label ), "%some%" ) );

			List<Request> results = entityManager.createQuery( query ).getResultList();
			assertThat( results ).hasSize( 1 );
			assertThat( results.get( 0 ).getId() ).isEqualTo( "r1" );
		} );
	}

	@Entity(name = "Asset")
	@Table(name = "assets")
	public static class Asset {
		@Id
		private String id;

		private String label;

		public Asset() {
		}

		public Asset(String id, String label) {
			this.id = id;
			this.label = label;
		}

		public String getId() {
			return id;
		}

		public String getLabel() {
			return label;
		}
	}

	@Entity(name = "BaseMapping")
	@Table(name = "base_mappings")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class BaseMapping {
		@Id
		private String id;

		public BaseMapping() {
		}

		public BaseMapping(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}
	}

	@Entity(name = "MappingA")
	@Table(name = "mappings_a")
	@PrimaryKeyJoinColumn(name = "mapping_id")
	public static class MappingA extends BaseMapping {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "asset_id")
		private Asset asset;

		public MappingA() {
		}

		public MappingA(String id, Asset asset) {
			super( id );
			this.asset = asset;
		}

		public Asset getAsset() {
			return asset;
		}
	}

	@Entity(name = "Request")
	@Table(name = "requests")
	public static class Request {
		@Id
		private String id;

		@OneToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "mapping_id", referencedColumnName = "id")
		private MappingA oneToOneMapping;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "mapping_id", referencedColumnName = "id", insertable = false, updatable = false)
		private MappingA manyToOneMapping;

		public Request() {
		}

		public Request(String id, MappingA mappingA) {
			this.id = id;
			this.oneToOneMapping = mappingA;
		}

		public String getId() {
			return id;
		}

		public MappingA getOneToOneMapping() {
			return oneToOneMapping;
		}

		public MappingA getManyToOneMapping() {
			return manyToOneMapping;
		}
	}
}
