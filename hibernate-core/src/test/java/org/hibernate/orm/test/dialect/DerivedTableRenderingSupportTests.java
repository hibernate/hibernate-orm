/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.util.List;

import org.hibernate.dialect.sql.ast.spi.DerivedColumnAliasing;
import org.hibernate.dialect.sql.ast.spi.DerivedTableKind;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingRequest;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.LateralReferenceStyle;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesTableRenderingStyle;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests the standard derived-table rendering profiles exposed to dialect
/// providers.
///
/// @since 8.0
/// @author Steve Ebersole
public class DerivedTableRenderingSupportTests {
	@Test
	void standardProfileSelectsKeywordOrQueryPartEmulation() {
		final DerivedTableRenderingSupport support = StandardDerivedTableRenderingSupport.STANDARD;

		assertThat( queryPartPlan( support, true, true ).lateralReferenceStyle() )
				.isEqualTo( LateralReferenceStyle.KEYWORD );
		assertThat( queryPartPlan( support, true, false ).lateralReferenceStyle() )
				.isEqualTo( LateralReferenceStyle.EMULATED_QUERY_PART );
		assertThat( queryPartPlan( support, false, false ).lateralReferenceStyle() )
				.isEqualTo( LateralReferenceStyle.IMPLICIT );
	}

	@Test
	void hanaProfileCombinesAliasingRenderingStyleAndTemporaryMode() {
		final DerivedTableRenderingPlan.QueryPart queryPart = queryPartPlan(
				StandardDerivedTableRenderingSupport.HANA,
				true,
				true
		);
		assertThat( queryPart.columnAliasing() ).isEqualTo( DerivedColumnAliasing.SELECT_LIST );
		assertThat( queryPart.renderingMode() ).isEqualTo( SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS );
		assertThat( queryPart.lateralReferenceStyle() ).isEqualTo( LateralReferenceStyle.KEYWORD );

		final DerivedTableRenderingPlan.QueryPart inlineCte = inlineCtePlan(
				StandardDerivedTableRenderingSupport.HANA,
				true,
				true
		);
		assertThat( inlineCte.lateralReferenceStyle() ).isEqualTo( LateralReferenceStyle.IMPLICIT );
		assertThat( inlineCte.renderingMode() ).isEqualTo( SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS );

		final DerivedTableRenderingPlan.Values values = valuesPlan(
				StandardDerivedTableRenderingSupport.HANA
		);
		assertThat( values.columnAliasing() ).isEqualTo( DerivedColumnAliasing.SELECT_LIST );
		assertThat( values.renderingStyle() ).isEqualTo( ValuesTableRenderingStyle.SELECT_UNION );
	}

	@Test
	void standardProfileEmulatesCorrelatedInlineCteWithoutLateralKeyword() {
		assertThat( inlineCtePlan( StandardDerivedTableRenderingSupport.STANDARD, true, true )
				.lateralReferenceStyle() ).isEqualTo( LateralReferenceStyle.KEYWORD );
		assertThat( inlineCtePlan( StandardDerivedTableRenderingSupport.STANDARD, true, false )
				.lateralReferenceStyle() ).isEqualTo( LateralReferenceStyle.EMULATED_QUERY_PART );
		assertThat( inlineCtePlan( StandardDerivedTableRenderingSupport.STANDARD, false, false )
				.lateralReferenceStyle() ).isEqualTo( LateralReferenceStyle.IMPLICIT );
	}

	@Test
	void familyProfilesCaptureStructuralVariations() {
		assertThat( queryPartPlan( StandardDerivedTableRenderingSupport.DB2_ZOS, false, false ).tablePrefix() )
				.isTrue();
		assertThat( queryPartPlan( StandardDerivedTableRenderingSupport.SPANNER, true, false ).lateralReferenceStyle() )
				.isEqualTo( LateralReferenceStyle.ARRAY_UNNEST );
		assertThat( queryPartPlan( StandardDerivedTableRenderingSupport.MYSQL_BEFORE_8, false, false ).columnAliasing() )
				.isEqualTo( DerivedColumnAliasing.SELECT_LIST );
		assertThat( queryPartPlan( StandardDerivedTableRenderingSupport.MYSQL_8, false, false ).columnAliasing() )
				.isEqualTo( DerivedColumnAliasing.DECLARATION );
		assertThat( valuesPlan( StandardDerivedTableRenderingSupport.ALTIBASE ).renderingMode() )
				.isEqualTo( SqlAstNodeRenderingMode.INLINE_PARAMETERS );
	}

	@Test
	void functionPlansCanSelectFamilySpecificAliasingAndLateralSyntax() {
		final DerivedTableRenderingPlan.Function oracle = functionPlan(
				StandardDerivedTableRenderingSupport.ORACLE,
				false,
				false
		);
		assertThat( oracle.columnAliasing() ).isEqualTo( DerivedColumnAliasing.IDENTIFICATION_VARIABLE_ONLY );

		final DerivedTableRenderingPlan.Function sqlServer = functionPlan(
				StandardDerivedTableRenderingSupport.SQL_SERVER,
				true,
				true
		);
		assertThat( sqlServer.lateralReferenceStyle() ).isEqualTo( LateralReferenceStyle.IMPLICIT );
	}

	private static DerivedTableRenderingPlan.QueryPart queryPartPlan(
			DerivedTableRenderingSupport support,
			boolean lateral,
			boolean supportsLateralKeyword) {
		return (DerivedTableRenderingPlan.QueryPart) support.determinePlan(
				new Request( DerivedTableKind.QUERY_PART, lateral, true, supportsLateralKeyword )
		);
	}

	private static DerivedTableRenderingPlan.Values valuesPlan(DerivedTableRenderingSupport support) {
		return (DerivedTableRenderingPlan.Values) support.determinePlan(
				new Request( DerivedTableKind.VALUES, false, false, false )
		);
	}

	private static DerivedTableRenderingPlan.QueryPart inlineCtePlan(
			DerivedTableRenderingSupport support,
			boolean lateral,
			boolean supportsLateralKeyword) {
		return (DerivedTableRenderingPlan.QueryPart) support.determinePlan(
				new Request( DerivedTableKind.INLINE_CTE, lateral, true, supportsLateralKeyword )
		);
	}

	private static DerivedTableRenderingPlan.Function functionPlan(
			DerivedTableRenderingSupport support,
			boolean lateral,
			boolean supportsLateralKeyword) {
		return (DerivedTableRenderingPlan.Function) support.determinePlan(
				new Request( DerivedTableKind.FUNCTION, lateral, false, supportsLateralKeyword )
		);
	}

	private record Request(
			DerivedTableKind kind,
			boolean lateral,
			boolean queryPartRoot,
			boolean supportsLateralKeyword) implements DerivedTableRenderingRequest {
		@Override
		public List<String> columnNames() {
			return List.of( "c1", "c2" );
		}
	}
}
