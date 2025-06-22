/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Marco Belladelli
 */
@SessionFactory(useCollectingStatementInspector = true)
@DomainModel(annotatedClasses = SameTableAliasInSubqueryWithEmbeddedTest.MasterDataFileEntity.class)
public class SameTableAliasInSubqueryWithEmbeddedTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MasterDataMetaData metadata1 = new MasterDataMetaData(
					"SYSTEM",
					"AT",
					TransportMode.INTERNATIONAL,
					"EUR",
					"NESTED_1"
			);
			final MasterDataFileEntity entity1 = new MasterDataFileEntity(
					new PrimaryKey(),
					metadata1,
					LocalDateTime.now(),
					MasterDataImportStatus.SUCCESS
			);
			session.persist( entity1 );
			final MasterDataMetaData metadata2 = new MasterDataMetaData(
					"PREMIUM",
					"DE",
					TransportMode.DOMESTIC,
					"EUR",
					"NESTED_2"
			);
			final MasterDataFileEntity entity2 = new MasterDataFileEntity(
					new PrimaryKey(),
					metadata2,
					LocalDateTime.now(),
					MasterDataImportStatus.SUCCESS
			);
			session.persist( entity2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createMutationQuery( "delete from MasterDataFileEntity" ).executeUpdate()
		);
	}


	@Test
	public void test(SessionFactoryScope scope) {
		final String jpql =
				"select mdf.id from MasterDataFileEntity as mdf " +
						"where mdf.dataImportStatus = 'SUCCESS' " +
						"  and mdf.metaData.country = :countryCode " +
						"  and mdf.metaData.nestedEmbeddable.nestedProperty = :nested " +
						"  and mdf.importFinishedAt = " +
						"   (select max(mdf.importFinishedAt) from MasterDataFileEntity as mdf " +
						"     where mdf.dataImportStatus = 'SUCCESS' " +
						"       and mdf.metaData.country = :countryCode " +
						"       and mdf.metaData.nestedEmbeddable.nestedProperty = :nested)";
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			TypedQuery<PrimaryKey> query = session.createQuery( jpql, PrimaryKey.class );
			query.setParameter( "countryCode", "DE" );
			query.setParameter( "nested", "NESTED_2" );
			assertNotNull( query.getSingleResult() );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "mdfe1_0", 6 );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "mdfe2_0", 5 );
		} );
	}

	@Test
	public void testNestedOnly(SessionFactoryScope scope) {
		final String jpql =
				"select mdf.id from MasterDataFileEntity as mdf " +
						"where mdf.metaData.nestedEmbeddable.nestedProperty = :nested " +
						"  and mdf.importFinishedAt = " +
						"   (select max(mdf.importFinishedAt) from MasterDataFileEntity as mdf " +
						"     where mdf.metaData.nestedEmbeddable.nestedProperty = :nested)";
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			TypedQuery<PrimaryKey> query = session.createQuery( jpql, PrimaryKey.class );
			query.setParameter( "nested", "NESTED_2" );
			assertNotNull( query.getSingleResult() );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "mdfe1_0", 4 );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "mdfe2_0", 3 );
		} );
	}

	@Embeddable
	public static class PrimaryKey implements Serializable {
		private String value;

		public PrimaryKey() {
			value = SafeRandomUUIDGenerator.safeRandomUUIDAsString();
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	public enum TransportMode {
		DOMESTIC, INTERNATIONAL;
	}

	@Embeddable
	public static class NestedEmbeddable {
		private String nestedProperty;

		public NestedEmbeddable() {
		}

		public NestedEmbeddable(String nestedProperty) {
			this.nestedProperty = nestedProperty;
		}
	}

	@Embeddable
	public static class MasterDataMetaData {
		private String country;

		@Enumerated(EnumType.STRING)
		private TransportMode transportMode;

		private String product;

		private String currencyCode;

		@Embedded
		private NestedEmbeddable nestedEmbeddable;

		protected MasterDataMetaData() {
		}

		public MasterDataMetaData(
				String product,
				String country,
				TransportMode transportMode,
				String currencyCode,
				String nestedProperty) {
			this.product = requireNonNull( product, "Product must not be null" );
			this.country = requireNonNull( country, "Country must not be null" );
			this.transportMode = requireNonNull( transportMode, "TransportMode must not be null" );
			this.currencyCode = requireNonNull( currencyCode, "CurrencyCode must not be null" );
			this.nestedEmbeddable = new NestedEmbeddable( nestedProperty );
		}
	}

	public enum MasterDataImportStatus {
		CREATED, FAILED, SUCCESS;
	}

	@Entity(name = "MasterDataFileEntity")
	@Table(name = "MasterDataFileEntity")
	public static class MasterDataFileEntity {
		@Id
		@AttributeOverride(name = "value", column = @Column(name = "id", nullable = false, length = 36))
		private PrimaryKey id;

		@Embedded
		private MasterDataMetaData metaData;

		private LocalDateTime importFinishedAt;

		@Enumerated(EnumType.STRING)
		private MasterDataImportStatus dataImportStatus;

		protected MasterDataFileEntity() {
		}

		public MasterDataFileEntity(
				PrimaryKey id,
				MasterDataMetaData metaData,
				LocalDateTime importFinishedAt,
				MasterDataImportStatus dataImportStatus) {
			this.id = id;
			this.metaData = metaData;
			this.importFinishedAt = importFinishedAt;
			this.dataImportStatus = dataImportStatus;
		}

		public PrimaryKey getId() {
			return id;
		}
	}

}
