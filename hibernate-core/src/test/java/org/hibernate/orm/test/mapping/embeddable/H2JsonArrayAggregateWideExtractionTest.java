/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import org.hibernate.Length;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.H2Dialect;
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

/**
 * Exercises H2's {@code array_agg(cast(... as ...))} rewrite path in
 * {@code H2AggregateSupport} for a JSON-aggregate array whose elements map to
 * a CLOB-ish type. Regression test for the {@code getNarrowCastTypeName} refactor:
 * <ul>
 *   <li>H2 rejects {@code array_agg(cast(x as clob))} ("funky results" per the
 *       code comment), so {@code H2Dialect.narrowCastType(CLOB) = "varchar"} maps
 *       the element to unsized {@code varchar};</li>
 *   <li>{@code H2AggregateSupport} must consume that via
 *       {@code getNarrowCastTypeName(element, column.toSize(), ...)}, not the
 *       plain {@link org.hibernate.dialect.function.array.DdlTypeHelper#getCastTypeName
 *       getCastTypeName};</li>
 *   <li>and the size overload must propagate the array column's length so long
 *       element values don't silently truncate.</li>
 * </ul>
 */
@JiraKey("HHH-20004")
@RequiresDialect(H2Dialect.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonAggregate.class)
@DomainModel(annotatedClasses = H2JsonArrayAggregateWideExtractionTest.JsonHolder.class)
@SessionFactory
public class H2JsonArrayAggregateWideExtractionTest {

	private static final String LONG_TEXT_A = "a".repeat( 5_000 );
	private static final String LONG_TEXT_B = "b".repeat( 10_000 );

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.persist(
						new JsonHolder(
								1L,
								new ArrayAggregate( new String[] { LONG_TEXT_A, LONG_TEXT_B } )
						)
				)
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testLongTextArrayRoundTrip(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final String[] longTexts = session.createQuery(
					"select h.aggregate.longTexts from JsonHolder h where h.id = 1",
					String[].class
			).getSingleResult();
			assertThat( longTexts ).containsExactly( LONG_TEXT_A, LONG_TEXT_B );
		} );
	}

	@Entity(name = "JsonHolder")
	public static class JsonHolder {

		@Id
		private Long id;

		@JdbcTypeCode(SqlTypes.JSON)
		private ArrayAggregate aggregate;

		public JsonHolder() {
		}

		public JsonHolder(Long id, ArrayAggregate aggregate) {
			this.id = id;
			this.aggregate = aggregate;
		}
	}

	@Embeddable
	public static class ArrayAggregate {

		// @Column(length = Length.LONG32) pushes the element type into
		// LONG32VARCHAR/CLOB territory, forcing H2AggregateSupport down the
		// narrow-cast path (array_agg of clob is rejected by H2).
		@Column(length = Length.LONG32)
		@JdbcTypeCode(SqlTypes.ARRAY)
		private String[] longTexts;

		public ArrayAggregate() {
		}

		public ArrayAggregate(String[] longTexts) {
			this.longTexts = longTexts;
		}
	}
}
