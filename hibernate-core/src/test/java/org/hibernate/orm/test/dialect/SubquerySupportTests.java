/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;


import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hibernate.dialect.sql.ast.spi.SubquerySupport.Feature.EXISTS_IN_SELECT;
import static org.hibernate.dialect.sql.ast.spi.SubquerySupport.Feature.IN_PREDICATE_LHS;
import static org.hibernate.dialect.sql.ast.spi.SubquerySupport.Feature.LATERAL;
import static org.hibernate.dialect.sql.ast.spi.SubquerySupport.Feature.MUTATION_JOIN;
import static org.hibernate.dialect.sql.ast.spi.SubquerySupport.Feature.MUTATION_TARGET_REFERENCE;
import static org.hibernate.dialect.sql.ast.spi.SubquerySupport.Feature.NESTED_CORRELATION;
import static org.hibernate.dialect.sql.ast.spi.SubquerySupport.Feature.OFFSET;
import static org.hibernate.dialect.sql.ast.spi.SubquerySupport.Feature.ORDER_BY;
import static org.hibernate.dialect.sql.ast.spi.SubquerySupport.Feature.SELECT_LIST;

/// Tests the immutable subquery-support provider contract and maintained
/// Dialect profiles.
///
/// @author Steve Ebersole
public class SubquerySupportTests {
	private static final SubquerySupport.Feature[] STANDARD_FEATURES = {
			SELECT_LIST,
			EXISTS_IN_SELECT,
			ORDER_BY,
			NESTED_CORRELATION,
			MUTATION_TARGET_REFERENCE,
			MUTATION_JOIN,
			IN_PREDICATE_LHS
	};

	@Test
	void constantsAndBuildersExposeIndependentImmutableFeatures() {
		assertFeatures( SubquerySupport.NONE );
		assertFeatures( SubquerySupport.STANDARD, STANDARD_FEATURES );

		final SubquerySupport mixed = SubquerySupport.builder( SubquerySupport.NONE )
				.features( SELECT_LIST, OFFSET, MUTATION_TARGET_REFERENCE, LATERAL )
				.build();
		assertFeatures( mixed, SELECT_LIST, OFFSET, MUTATION_TARGET_REFERENCE, LATERAL );
		assertThat( mixed.supports( EXISTS_IN_SELECT ) ).isFalse();
		assertThat( mixed.supports( ORDER_BY ) ).isFalse();
		assertThat( mixed.supports( MUTATION_JOIN ) ).isFalse();

		final SubquerySupport copied = SubquerySupport.builder( SubquerySupport.STANDARD )
				.feature( SELECT_LIST, false )
				.feature( EXISTS_IN_SELECT, false )
				.feature( OFFSET, true )
				.build();
		assertFeatures(
				copied,
				ORDER_BY,
				OFFSET,
				NESTED_CORRELATION,
				MUTATION_TARGET_REFERENCE,
				MUTATION_JOIN,
				IN_PREDICATE_LHS
		);
		assertFeatures( SubquerySupport.STANDARD, STANDARD_FEATURES );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> mixed.getFeatures().add( ORDER_BY ) );
	}

	@Test
	@SuppressWarnings("NullAway")
	void buildersAndQueriesRejectNullInputs() {
		assertThatIllegalArgumentException().isThrownBy( () -> SubquerySupport.builder( null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> SubquerySupport.builder().features( (SubquerySupport.Feature[]) null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> SubquerySupport.builder().features( SELECT_LIST, null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> SubquerySupport.builder().feature( null, true ) );
		assertThatIllegalArgumentException().isThrownBy( () -> SubquerySupport.STANDARD.supports( null ) );
	}

	@Test
	void maintainedProfilesPreserveIndependentProviderValues() {
		assertFeatures( new Dialect( DatabaseVersion.make( 1 ) ) {
		}.getSubquerySupport(), STANDARD_FEATURES );
		assertFeatures(
				new H2Dialect(),
				SELECT_LIST, EXISTS_IN_SELECT, ORDER_BY, OFFSET, NESTED_CORRELATION,
				MUTATION_TARGET_REFERENCE, IN_PREDICATE_LHS
		);
		assertFeatures(
				new MySQLDialect( DatabaseVersion.make( 8, 0, 13 ) ),
				SELECT_LIST, EXISTS_IN_SELECT, ORDER_BY, OFFSET, MUTATION_JOIN, IN_PREDICATE_LHS
		);
		assertFeatures(
				new MySQLDialect( DatabaseVersion.make( 8, 0, 14 ) ),
				SELECT_LIST, EXISTS_IN_SELECT, ORDER_BY, OFFSET, MUTATION_JOIN, IN_PREDICATE_LHS, LATERAL
		);
		assertThat( new MariaDBDialect().getSubquerySupport().supports( LATERAL ) ).isFalse();
		assertFeatures(
				new OracleDialect( DatabaseVersion.make( 19 ) ),
				SELECT_LIST, ORDER_BY, OFFSET, MUTATION_TARGET_REFERENCE, MUTATION_JOIN,
				IN_PREDICATE_LHS, LATERAL
		);
		assertOffsetAndLateral( new DB2Dialect() );
		assertOffsetAndLateral( new PostgreSQLDialect() );
		assertOffsetAndLateral( new CockroachDialect() );
		assertOffsetAndLateral( new HANADialect() );
		assertOffsetAndLateral( new HSQLDialect() );
		assertOffsetAndLateral( new SpannerDialect() );
		assertThat( new SpannerPostgreSQLDialect().getSubquerySupport().supports( OFFSET ) ).isTrue();
		assertThat( new SpannerPostgreSQLDialect().getSubquerySupport().supports( LATERAL ) ).isFalse();
		assertThat( new SQLServerDialect().getSubquerySupport().supports( EXISTS_IN_SELECT ) ).isFalse();
		assertOffsetAndLateral( new SQLServerDialect() );
		assertThat( new SybaseASEDialect().getSubquerySupport().supports( EXISTS_IN_SELECT ) ).isFalse();
		assertThat( new SybaseASEDialect().getSubquerySupport().supports( ORDER_BY ) ).isFalse();
	}

	private static void assertOffsetAndLateral(Dialect dialect) {
		assertThat( dialect.getSubquerySupport().supports( OFFSET ) ).isTrue();
		assertThat( dialect.getSubquerySupport().supports( LATERAL ) ).isTrue();
	}

	private static void assertFeatures(Dialect dialect, SubquerySupport.Feature... features) {
		assertFeatures( dialect.getSubquerySupport(), features );
	}

	private static void assertFeatures(SubquerySupport support, SubquerySupport.Feature... features) {
		assertThat( support.getFeatures() ).containsExactlyInAnyOrder( features );
	}
}
