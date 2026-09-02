/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.util.List;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.sql.ast.spi.query.select.QueryGroup;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcSelect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hibernate.dialect.sql.ast.spi.SetOperationSupport.Capability.DUPLICATE_SELECT_ITEMS;
import static org.hibernate.dialect.sql.ast.spi.SetOperationSupport.Capability.SIMPLE_QUERY_GROUPING;
import static org.hibernate.dialect.sql.ast.spi.SetOperationSupport.Capability.UNION_IN_SUBQUERY;
import static org.hibernate.query.sqm.SetOperator.EXCEPT;
import static org.hibernate.query.sqm.SetOperator.EXCEPT_ALL;
import static org.hibernate.query.sqm.SetOperator.INTERSECT;
import static org.hibernate.query.sqm.SetOperator.INTERSECT_ALL;
import static org.hibernate.query.sqm.SetOperator.UNION;
import static org.hibernate.query.sqm.SetOperator.UNION_ALL;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests the immutable set-operation-support provider contract, maintained
/// Dialect profiles, grammar boundaries, spelling, and rendering guard.
///
/// @author Steve Ebersole
public class SetOperationSupportTests {
	@Test
	void constantsAndBuildersExposeIndependentImmutableSets() {
		assertProfile( SetOperationSupport.NONE, new SetOperator[0] );
		assertCapabilities( SetOperationSupport.NONE );
		assertProfile( SetOperationSupport.STANDARD, SetOperator.values() );
		assertCapabilities(
				SetOperationSupport.STANDARD,
				UNION_IN_SUBQUERY,
				DUPLICATE_SELECT_ITEMS,
				SIMPLE_QUERY_GROUPING
		);

		final SetOperationSupport copied = SetOperationSupport.builder( SetOperationSupport.STANDARD )
				.operator( UNION_ALL, false )
				.operator( INTERSECT, false )
				.capability( UNION_IN_SUBQUERY, false )
				.build();
		assertProfile( copied, UNION, INTERSECT_ALL, EXCEPT, EXCEPT_ALL );
		assertCapabilities( copied, DUPLICATE_SELECT_ITEMS, SIMPLE_QUERY_GROUPING );
		assertProfile( SetOperationSupport.STANDARD, SetOperator.values() );

		final SetOperationSupport mixed = SetOperationSupport.builder( SetOperationSupport.NONE )
				.operators( UNION, UNION_ALL, EXCEPT )
				.capabilities( SIMPLE_QUERY_GROUPING )
				.build();
		assertProfile( mixed, UNION, UNION_ALL, EXCEPT );
		assertCapabilities( mixed, SIMPLE_QUERY_GROUPING );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> mixed.getSupportedOperators().add( INTERSECT ) );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> mixed.getCapabilities().add( DUPLICATE_SELECT_ITEMS ) );
	}

	@Test
	@SuppressWarnings("NullAway")
	void buildersAndQueriesRejectNullInputs() {
		assertThatIllegalArgumentException().isThrownBy( () -> SetOperationSupport.builder( null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> SetOperationSupport.builder().operators( (SetOperator[]) null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> SetOperationSupport.builder().operators( UNION, null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> SetOperationSupport.builder().operator( null, true ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> SetOperationSupport.builder()
						.capabilities( (SetOperationSupport.Capability[]) null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> SetOperationSupport.builder().capabilities( UNION_IN_SUBQUERY, null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> SetOperationSupport.builder().capability( null, true ) );
		assertThatIllegalArgumentException().isThrownBy( () -> SetOperationSupport.STANDARD.supports( (SetOperator) null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> SetOperationSupport.STANDARD.supports( (SetOperationSupport.Capability) null ) );
	}

	@Test
	void maintainedProfilesMatchTheAuditedGrammarMatrix() {
		assertProfile( new Dialect( DatabaseVersion.make( 1 ) ) {
		}.getSetOperationSupport(), SetOperator.values() );

		// H2 2.4.240 parser verification: INTERSECT ALL and EXCEPT ALL are rejected.
		final H2Dialect h2 = new H2Dialect();
		assertProfile( h2, UNION, UNION_ALL, INTERSECT, EXCEPT );
		assertCapabilities(
				h2.getSetOperationSupport(),
				UNION_IN_SUBQUERY,
				DUPLICATE_SELECT_ITEMS,
				SIMPLE_QUERY_GROUPING
		);

		// MySQL 8.0.31 release notes add INTERSECT/EXCEPT, each with DISTINCT and ALL.
		assertProfile( new MySQLDialect( DatabaseVersion.make( 8, 0, 30 ) ), UNION, UNION_ALL );
		final MySQLDialect mysql31 = new MySQLDialect( DatabaseVersion.make( 8, 0, 31 ) );
		assertProfile( mysql31, SetOperator.values() );
		assertCapabilities(
				mysql31.getSetOperationSupport(),
				UNION_IN_SUBQUERY,
				DUPLICATE_SELECT_ITEMS,
				SIMPLE_QUERY_GROUPING
		);

		// MariaDB documents INTERSECT/EXCEPT since 10.3 and their ALL forms since 10.5.
		final MariaDBDialect mariaDB = new MariaDBDialect();
		assertProfile( mariaDB.getSetOperationSupport(), SetOperator.values() );
		assertCapabilities( mariaDB.getSetOperationSupport(), UNION_IN_SUBQUERY, SIMPLE_QUERY_GROUPING );

		// Oracle 21 adds the ALL forms; earlier releases spell EXCEPT as MINUS.
		final OracleDialect oracle19 = new OracleDialect( DatabaseVersion.make( 19 ) );
		assertProfile( oracle19.getSetOperationSupport(), UNION, UNION_ALL, INTERSECT, EXCEPT );
		assertCapabilities( oracle19.getSetOperationSupport(), UNION_IN_SUBQUERY, SIMPLE_QUERY_GROUPING );
		assertThat( oracle19.getSetOperatorSqlString( EXCEPT ) ).isEqualTo( "minus" );
		assertThat( render( oracle19, EXCEPT ) ).contains( "minus" );
		final OracleDialect oracle21 = new OracleDialect( DatabaseVersion.make( 21 ) );
		assertProfile( oracle21.getSetOperationSupport(), SetOperator.values() );
		assertThat( oracle21.getSetOperatorSqlString( EXCEPT ) ).isEqualTo( "except" );

		// Transact-SQL grammar has ALL only for UNION.
		final SQLServerDialect sqlServer = new SQLServerDialect();
		assertProfile( sqlServer.getSetOperationSupport(), UNION, UNION_ALL, INTERSECT, EXCEPT );
		assertCapabilities( sqlServer.getSetOperationSupport(), UNION_IN_SUBQUERY, DUPLICATE_SELECT_ITEMS );

		// Hibernate retains its established ASE INTERSECT and UNION-subquery restrictions.
		final SybaseASEDialect sybase = new SybaseASEDialect();
		assertProfile( sybase.getSetOperationSupport(), UNION, UNION_ALL, EXCEPT );
		assertCapabilities( sybase.getSetOperationSupport(), DUPLICATE_SELECT_ITEMS, SIMPLE_QUERY_GROUPING );
	}

	@Test
	void allSixOperatorsRenderIndependentlyAndUnsupportedOperatorsFailClearly() {
		for ( SetOperator operator : SetOperator.values() ) {
			assertThat( render( SetOperationSupport.STANDARD, operator ) )
					.contains( operator.sqlString() );
		}

		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> render( SetOperationSupport.NONE, INTERSECT_ALL ) )
				.withMessageContaining( "INTERSECT_ALL" )
				.withMessageContaining( "not supported by" );
	}

	private static String render(SetOperationSupport support, SetOperator operator) {
		return render( new Dialect( DatabaseVersion.make( 1 ) ) {
			@Override
			public SetOperationSupport getSetOperationSupport() {
				return support;
			}
		}, operator );
	}

	private static String render(Dialect dialect, SetOperator operator) {
		final SessionFactoryImplementor sessionFactory = mock( SessionFactoryImplementor.class );
		final JdbcServices jdbcServices = mock( JdbcServices.class );
		when( sessionFactory.getJdbcServices() ).thenReturn( jdbcServices );
		when( jdbcServices.getDialect() ).thenReturn( dialect );
		final TestingTranslator translator = new TestingTranslator( new SqlAstTranslationRequest.Select(
				sessionFactory,
				new SelectStatement( new QuerySpec( true ) )
		) );
		translator.render( new QueryGroup(
				true,
				operator,
				List.of( new QuerySpec( false ), new QuerySpec( false ) )
		) );
		return translator.renderedSql();
	}

	private static void assertProfile(Dialect dialect, SetOperator... operators) {
		assertProfile( dialect.getSetOperationSupport(), operators );
	}

	private static void assertProfile(SetOperationSupport profile, SetOperator... operators) {
		assertThat( profile.getSupportedOperators() ).containsExactlyInAnyOrder( operators );
	}

	private static void assertCapabilities(
			SetOperationSupport profile,
			SetOperationSupport.Capability... capabilities) {
		assertThat( profile.getCapabilities() ).containsExactlyInAnyOrder( capabilities );
	}

	private static class TestingTranslator extends AbstractSqlAstTranslator<JdbcSelect> {
		private TestingTranslator(SqlAstTranslationRequest.Select request) {
			super( request );
		}

		private void render(QueryGroup queryGroup) {
			visitQueryGroup( queryGroup );
		}

		private String renderedSql() {
			return getSql();
		}
	}
}
