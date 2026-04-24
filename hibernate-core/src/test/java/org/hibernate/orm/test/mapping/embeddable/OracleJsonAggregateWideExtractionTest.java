/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import org.hibernate.Length;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-20004")
@RequiresDialect(OracleDialect.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonAggregate.class)
@DomainModel(annotatedClasses = OracleJsonAggregateWideExtractionTest.JsonHolder.class)
@SessionFactory
public class OracleJsonAggregateWideExtractionTest {

	private static final String LONG_TEXT = "x".repeat( 5000 );
	private static final byte[] BINARY_DATA = createBinaryData( 3000 );

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.persist(
						new JsonHolder(
								1L,
								new LargeJsonAggregate( LONG_TEXT, BINARY_DATA )
						)
				)
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testLargeBinaryRoundTrip(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final byte[] binaryData = session.createQuery(
					"select h.aggregate.binaryData from JsonHolder h where h.id = 1",
					byte[].class
			).getSingleResult();
			assertThat( binaryData ).containsExactly( BINARY_DATA );
		} );
	}

	@Test
	public void testLongTextRoundTrip(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final String longText = session.createQuery(
					"select h.aggregate.longText from JsonHolder h where h.id = 1",
					String.class
			).getSingleResult();
			assertThat( longText ).isEqualTo( LONG_TEXT );
		} );
	}

	private static byte[] createBinaryData(int size) {
		final byte[] data = new byte[size];
		for ( int i = 0; i < size; i++ ) {
			data[i] = (byte) ( i % 251 );
		}
		return data;
	}

	@Entity(name = "JsonHolder")
	public static class JsonHolder {

		@Id
		private Long id;

		@JdbcTypeCode(SqlTypes.JSON)
		private LargeJsonAggregate aggregate;

		public JsonHolder() {
		}

		public JsonHolder(Long id, LargeJsonAggregate aggregate) {
			this.id = id;
			this.aggregate = aggregate;
		}
	}

	@Embeddable
	public static class LargeJsonAggregate {

		@Column(length = Length.LONG32)
		private String longText;

		@JdbcTypeCode(SqlTypes.BLOB)
		private byte[] binaryData;

		public LargeJsonAggregate() {
		}

		public LargeJsonAggregate(String longText, byte[] binaryData) {
			this.longText = longText;
			this.binaryData = binaryData;
		}
	}
}
