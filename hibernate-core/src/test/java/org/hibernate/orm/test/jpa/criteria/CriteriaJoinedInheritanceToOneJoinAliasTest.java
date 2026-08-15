/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.List;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A Criteria path traversal through a to-one association whose target uses JOINED inheritance,
 * where the foreign key targets a column of a <em>superclass</em> table, generates SQL with a
 * mismatched table alias in the join ON clause (e.g. {@code join mappings_a m1_0 on m1_1.id=...}
 * where {@code m1_1} — the root-table alias — is never declared). Both {@code @OneToOne} and
 * {@code @ManyToOne} are affected, with and without a discriminator column, and whether the
 * referenced column is the root primary key or an ordinary unique column.
 */
@Jpa(
		annotatedClasses = {
				CriteriaJoinedInheritanceToOneJoinAliasTest.Asset.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.BaseMapping.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.MappingA.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.RequestWithOneToOne.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.RequestWithManyToOne.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.BaseMappingWithDiscriminator.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.MappingWithDiscriminator.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.RequestWithDiscriminator.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.BaseMappingWithCode.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.MappingWithCode.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.RequestWithPropertyRef.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.DeepBaseMapping.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.DeepIntermediateMapping.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.DeepLeafMapping.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.RequestWithDeepMapping.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.BaseMappingWithSecondaryTable.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.MappingWithSecondaryTable.class,
				CriteriaJoinedInheritanceToOneJoinAliasTest.RequestWithSecondaryTableRef.class,
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

			entityManager.persist( new RequestWithOneToOne( "r1", mapping ) );
			entityManager.persist( new RequestWithManyToOne( "r2", mapping ) );

			MappingWithDiscriminator discriminatorMapping = new MappingWithDiscriminator( "m2", asset );
			entityManager.persist( discriminatorMapping );
			entityManager.persist( new RequestWithDiscriminator( "r3", discriminatorMapping ) );

			MappingWithCode codeMapping = new MappingWithCode( "m3", "c1", asset );
			entityManager.persist( codeMapping );
			entityManager.persist( new RequestWithPropertyRef( "r4", codeMapping ) );

			DeepLeafMapping deepMapping = new DeepLeafMapping( "m4", asset );
			entityManager.persist( deepMapping );
			entityManager.persist( new RequestWithDeepMapping( "r5", deepMapping ) );

			MappingWithSecondaryTable secondaryTableMapping = new MappingWithSecondaryTable( "m5", "e1", asset );
			entityManager.persist( secondaryTableMapping );
			entityManager.persist( new RequestWithSecondaryTableRef( "r6", secondaryTableMapping ) );
		} );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "delete from RequestWithOneToOne" ).executeUpdate();
			entityManager.createQuery( "delete from RequestWithManyToOne" ).executeUpdate();
			entityManager.createQuery( "delete from RequestWithDiscriminator" ).executeUpdate();
			entityManager.createQuery( "delete from RequestWithPropertyRef" ).executeUpdate();
			entityManager.createQuery( "delete from MappingA" ).executeUpdate();
			entityManager.createQuery( "delete from BaseMapping" ).executeUpdate();
			entityManager.createQuery( "delete from MappingWithDiscriminator" ).executeUpdate();
			entityManager.createQuery( "delete from BaseMappingWithDiscriminator" ).executeUpdate();
			entityManager.createQuery( "delete from MappingWithCode" ).executeUpdate();
			entityManager.createQuery( "delete from BaseMappingWithCode" ).executeUpdate();
			entityManager.createQuery( "delete from RequestWithDeepMapping" ).executeUpdate();
			entityManager.createQuery( "delete from DeepLeafMapping" ).executeUpdate();
			entityManager.createQuery( "delete from DeepBaseMapping" ).executeUpdate();
			entityManager.createQuery( "delete from RequestWithSecondaryTableRef" ).executeUpdate();
			entityManager.createQuery( "delete from MappingWithSecondaryTable" ).executeUpdate();
			entityManager.createQuery( "delete from BaseMappingWithSecondaryTable" ).executeUpdate();
			entityManager.createQuery( "delete from Asset" ).executeUpdate();
		} );
	}

	@Test
	public void testImplicitJoinThroughOneToOne(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<RequestWithOneToOne> query = cb.createQuery( RequestWithOneToOne.class );
			Root<RequestWithOneToOne> root = query.from( RequestWithOneToOne.class );

			Path<String> label = root
					.get( "mapping" )
					.get( "asset" )
					.get( "label" );
			query.where( cb.like( cb.lower( label ), "%some%" ) );

			List<RequestWithOneToOne> results = entityManager.createQuery( query ).getResultList();
			assertThat( results ).hasSize( 1 );
			assertThat( results.get( 0 ).getId() ).isEqualTo( "r1" );
		} );
	}

	@Test
	public void testImplicitJoinThroughManyToOne(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<RequestWithManyToOne> query = cb.createQuery( RequestWithManyToOne.class );
			Root<RequestWithManyToOne> root = query.from( RequestWithManyToOne.class );

			Path<String> label = root
					.get( "mapping" )
					.get( "asset" )
					.get( "label" );
			query.where( cb.like( cb.lower( label ), "%some%" ) );

			List<RequestWithManyToOne> results = entityManager.createQuery( query ).getResultList();
			assertThat( results ).hasSize( 1 );
			assertThat( results.get( 0 ).getId() ).isEqualTo( "r2" );
		} );
	}

	@Test
	public void testImplicitJoinWithDiscriminatorColumn(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<RequestWithDiscriminator> query = cb.createQuery( RequestWithDiscriminator.class );
			Root<RequestWithDiscriminator> root = query.from( RequestWithDiscriminator.class );

			Path<String> label = root
					.get( "mapping" )
					.get( "asset" )
					.get( "label" );
			query.where( cb.like( cb.lower( label ), "%some%" ) );

			List<RequestWithDiscriminator> results = entityManager.createQuery( query ).getResultList();
			assertThat( results ).hasSize( 1 );
			assertThat( results.get( 0 ).getId() ).isEqualTo( "r3" );
		} );
	}

	@Test
	public void testImplicitJoinThroughNonKeyPropertyRef(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<RequestWithPropertyRef> query = cb.createQuery( RequestWithPropertyRef.class );
			Root<RequestWithPropertyRef> root = query.from( RequestWithPropertyRef.class );

			Path<String> label = root
					.get( "mapping" )
					.get( "asset" )
					.get( "label" );
			query.where( cb.like( cb.lower( label ), "%some%" ) );

			List<RequestWithPropertyRef> results = entityManager.createQuery( query ).getResultList();
			assertThat( results ).hasSize( 1 );
			assertThat( results.get( 0 ).getId() ).isEqualTo( "r4" );
		} );
	}

	/**
	 * The entity to register a use for is the one whose <em>own</em> table is the referenced table,
	 * which in a hierarchy deeper than two levels is not the immediate superclass. Widening the
	 * search to everything in a persister's table span would match the intermediate class first —
	 * its span covers the inherited root table too — and pruning, which retains one table per
	 * registered entity name, would then keep the intermediate table and still drop the root.
	 */
	@Test
	public void testImplicitJoinThroughThreeLevelHierarchy(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<RequestWithDeepMapping> query = cb.createQuery( RequestWithDeepMapping.class );
			Root<RequestWithDeepMapping> root = query.from( RequestWithDeepMapping.class );

			Path<String> label = root
					.get( "mapping" )
					.get( "asset" )
					.get( "label" );
			query.where( cb.like( cb.lower( label ), "%some%" ) );

			List<RequestWithDeepMapping> results = entityManager.createQuery( query ).getResultList();
			assertThat( results ).hasSize( 1 );
			assertThat( results.get( 0 ).getId() ).isEqualTo( "r5" );
		} );
	}

	/**
	 * The same three-level hierarchy reached through an explicit outer join, which prunes the
	 * target's table group along a different path than the implicit inner join above.
	 */
	@Test
	public void testOuterJoinThroughThreeLevelHierarchy(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<String> results = entityManager.createQuery(
					"select r.id from RequestWithDeepMapping r left join r.mapping m"
							+ " where lower(m.asset.label) like '%some%'", String.class )
					.getResultList();

			assertThat( results ).containsExactly( "r5" );
		} );
	}

	/**
	 * A foreign key targeting a column on a {@linkplain jakarta.persistence.SecondaryTable secondary
	 * table} of a superclass. This is <em>not</em> a regression test for the entity name usage
	 * registration: the generated SQL is identical with and without it, because
	 * {@code JoinedSubclassEntityPersister} retains joins to secondary tables independently of any
	 * registered use. It is here to document that boundary — registering a use could not retain such
	 * a join anyway, since pruning translates a registered entity name into that entity's own primary
	 * table only.
	 */
	@Test
	public void testForeignKeyTargetingSecondaryTableOfSuperclass(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<RequestWithSecondaryTableRef> query =
					cb.createQuery( RequestWithSecondaryTableRef.class );
			Root<RequestWithSecondaryTableRef> root = query.from( RequestWithSecondaryTableRef.class );

			Path<String> label = root
					.get( "mapping" )
					.get( "asset" )
					.get( "label" );
			query.where( cb.like( cb.lower( label ), "%some%" ) );

			List<RequestWithSecondaryTableRef> results = entityManager.createQuery( query ).getResultList();
			assertThat( results ).hasSize( 1 );
			assertThat( results.get( 0 ).getId() ).isEqualTo( "r6" );
		} );
	}

	/**
	 * Retaining the superclass table must stay confined to queries whose join predicate actually
	 * references it: a query that only needs the subclass table must still prune the root-table join.
	 */
	@Test
	public void testRootTableStillPrunedWhenNotReferenced(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SQLStatementInspector inspector = scope.getCollectingStatementInspector();
			inspector.clear();

			entityManager.createQuery(
					"select m.id from MappingA m where m.asset.label like '%some%'", String.class )
					.getResultList();

			assertThat( inspector.getSqlQueries() ).hasSize( 1 );
			assertThat( inspector.getSqlQueries().get( 0 ) ).doesNotContain( "base_mappings" );
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

	@Entity(name = "RequestWithOneToOne")
	@Table(name = "requests_one_to_one")
	public static class RequestWithOneToOne {
		@Id
		private String id;

		// referencedColumnName = "id" is essential to the reproducer: it makes the foreign key target the
		// identifier, which for JOINED inheritance lives on the root table (base_mappings) rather than on
		// the leaf table (mappings_a). That is what puts the root-table alias into the join predicate.
		@OneToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "mapping_id", referencedColumnName = "id")
		private MappingA mapping;

		public RequestWithOneToOne() {
		}

		public RequestWithOneToOne(String id, MappingA mapping) {
			this.id = id;
			this.mapping = mapping;
		}

		public String getId() {
			return id;
		}

		public MappingA getMapping() {
			return mapping;
		}
	}

	@Entity(name = "RequestWithManyToOne")
	@Table(name = "requests_many_to_one")
	public static class RequestWithManyToOne {
		@Id
		private String id;

		// See RequestWithOneToOne#mapping: referencedColumnName = "id" is what triggers the bug.
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "mapping_id", referencedColumnName = "id")
		private MappingA mapping;

		public RequestWithManyToOne() {
		}

		public RequestWithManyToOne(String id, MappingA mapping) {
			this.id = id;
			this.mapping = mapping;
		}

		public String getId() {
			return id;
		}

		public MappingA getMapping() {
			return mapping;
		}
	}

	// ~~~~~~~~~~ JOINED hierarchy with a physical discriminator column ~~~~~~~~~~
	// A discriminator does not avoid the problem: the referenced column still belongs to the
	// root table, so the join predicate still qualifies it with the root-table alias.

	@Entity(name = "BaseMappingWithDiscriminator")
	@Table(name = "disc_base_mappings")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn(name = "mapping_type")
	@DiscriminatorValue("base")
	public static class BaseMappingWithDiscriminator {
		@Id
		private String id;

		public BaseMappingWithDiscriminator() {
		}

		public BaseMappingWithDiscriminator(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}
	}

	@Entity(name = "MappingWithDiscriminator")
	@Table(name = "disc_mappings_a")
	@PrimaryKeyJoinColumn(name = "mapping_id")
	@DiscriminatorValue("a")
	public static class MappingWithDiscriminator extends BaseMappingWithDiscriminator {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "asset_id")
		private Asset asset;

		public MappingWithDiscriminator() {
		}

		public MappingWithDiscriminator(String id, Asset asset) {
			super( id );
			this.asset = asset;
		}

		public Asset getAsset() {
			return asset;
		}
	}

	@Entity(name = "RequestWithDiscriminator")
	@Table(name = "disc_requests")
	public static class RequestWithDiscriminator {
		@Id
		private String id;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "mapping_id", referencedColumnName = "id")
		private MappingWithDiscriminator mapping;

		public RequestWithDiscriminator() {
		}

		public RequestWithDiscriminator(String id, MappingWithDiscriminator mapping) {
			this.id = id;
			this.mapping = mapping;
		}

		public String getId() {
			return id;
		}

		public MappingWithDiscriminator getMapping() {
			return mapping;
		}
	}

	// ~~~~~~~~~~ foreign key targeting an ordinary (non-key) column of the root table ~~~~~~~~~~
	// Here the reference genuinely is not to the primary key, yet the outcome is the same: the
	// target column lives on the root table, so the root-table alias appears in the predicate.

	@Entity(name = "BaseMappingWithCode")
	@Table(name = "code_base_mappings")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class BaseMappingWithCode {
		@Id
		private String id;

		@Column(name = "code", unique = true)
		private String code;

		public BaseMappingWithCode() {
		}

		public BaseMappingWithCode(String id, String code) {
			this.id = id;
			this.code = code;
		}

		public String getId() {
			return id;
		}

		public String getCode() {
			return code;
		}
	}

	@Entity(name = "MappingWithCode")
	@Table(name = "code_mappings_a")
	@PrimaryKeyJoinColumn(name = "mapping_id")
	public static class MappingWithCode extends BaseMappingWithCode {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "asset_id")
		private Asset asset;

		public MappingWithCode() {
		}

		public MappingWithCode(String id, String code, Asset asset) {
			super( id, code );
			this.asset = asset;
		}

		public Asset getAsset() {
			return asset;
		}
	}

	@Entity(name = "RequestWithPropertyRef")
	@Table(name = "code_requests")
	public static class RequestWithPropertyRef {
		@Id
		private String id;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "mapping_code", referencedColumnName = "code")
		private MappingWithCode mapping;

		public RequestWithPropertyRef() {
		}

		public RequestWithPropertyRef(String id, MappingWithCode mapping) {
			this.id = id;
			this.mapping = mapping;
		}

		public String getId() {
			return id;
		}

		public MappingWithCode getMapping() {
			return mapping;
		}
	}

	// ~~~~~~~~~~ JOINED hierarchy three levels deep, foreign key targeting the root table ~~~~~~~~~~
	// The entity whose own table the foreign key targets is two levels up, so the intermediate class
	// must be skipped: it maps deep_intermediate_mappings, not the referenced deep_base_mappings.

	@Entity(name = "DeepBaseMapping")
	@Table(name = "deep_base_mappings")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class DeepBaseMapping {
		@Id
		private String id;

		public DeepBaseMapping() {
		}

		public DeepBaseMapping(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}
	}

	@Entity(name = "DeepIntermediateMapping")
	@Table(name = "deep_intermediate_mappings")
	@PrimaryKeyJoinColumn(name = "base_id")
	public static class DeepIntermediateMapping extends DeepBaseMapping {
		public DeepIntermediateMapping() {
		}

		public DeepIntermediateMapping(String id) {
			super( id );
		}
	}

	@Entity(name = "DeepLeafMapping")
	@Table(name = "deep_leaf_mappings")
	@PrimaryKeyJoinColumn(name = "intermediate_id")
	public static class DeepLeafMapping extends DeepIntermediateMapping {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "asset_id")
		private Asset asset;

		public DeepLeafMapping() {
		}

		public DeepLeafMapping(String id, Asset asset) {
			super( id );
			this.asset = asset;
		}

		public Asset getAsset() {
			return asset;
		}
	}

	@Entity(name = "RequestWithDeepMapping")
	@Table(name = "deep_requests")
	public static class RequestWithDeepMapping {
		@Id
		private String id;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "mapping_id", referencedColumnName = "id")
		private DeepLeafMapping mapping;

		public RequestWithDeepMapping() {
		}

		public RequestWithDeepMapping(String id, DeepLeafMapping mapping) {
			this.id = id;
			this.mapping = mapping;
		}

		public String getId() {
			return id;
		}

		public DeepLeafMapping getMapping() {
			return mapping;
		}
	}

	// ~~~~~~~~~~ foreign key targeting a column on a secondary table of the superclass ~~~~~~~~~~
	// See testForeignKeyTargetingSecondaryTableOfSuperclass: this shape is unaffected by the entity
	// name usage registration, because joins to secondary tables are retained by pruning regardless.

	@Entity(name = "BaseMappingWithSecondaryTable")
	@Table(name = "sec_base_mappings")
	@SecondaryTable(name = "sec_base_mapping_details")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class BaseMappingWithSecondaryTable {
		@Id
		private String id;

		@Column(table = "sec_base_mapping_details", name = "ext_code", unique = true)
		private String extCode;

		public BaseMappingWithSecondaryTable() {
		}

		public BaseMappingWithSecondaryTable(String id, String extCode) {
			this.id = id;
			this.extCode = extCode;
		}

		public String getId() {
			return id;
		}

		public String getExtCode() {
			return extCode;
		}
	}

	@Entity(name = "MappingWithSecondaryTable")
	@Table(name = "sec_mappings_a")
	@PrimaryKeyJoinColumn(name = "mapping_id")
	public static class MappingWithSecondaryTable extends BaseMappingWithSecondaryTable {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "asset_id")
		private Asset asset;

		public MappingWithSecondaryTable() {
		}

		public MappingWithSecondaryTable(String id, String extCode, Asset asset) {
			super( id, extCode );
			this.asset = asset;
		}

		public Asset getAsset() {
			return asset;
		}
	}

	@Entity(name = "RequestWithSecondaryTableRef")
	@Table(name = "sec_requests")
	public static class RequestWithSecondaryTableRef {
		@Id
		private String id;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "mapping_ext_code", referencedColumnName = "ext_code")
		private MappingWithSecondaryTable mapping;

		public RequestWithSecondaryTableRef() {
		}

		public RequestWithSecondaryTableRef(String id, MappingWithSecondaryTable mapping) {
			this.id = id;
			this.mapping = mapping;
		}

		public String getId() {
			return id;
		}

		public MappingWithSecondaryTable getMapping() {
			return mapping;
		}
	}
}
