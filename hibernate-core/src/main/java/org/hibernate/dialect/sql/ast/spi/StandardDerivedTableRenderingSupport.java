/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.Objects;

import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;

import jakarta.annotation.Nullable;

/// Standard and family derived-table rendering profiles.
///
/// @since 8.0
/// @author Steve Ebersole
public final class StandardDerivedTableRenderingSupport implements DerivedTableRenderingSupport {
	public static final DerivedTableRenderingSupport STANDARD = profile(
			DerivedColumnAliasing.DECLARATION,
			DerivedColumnAliasing.DECLARATION,
			DerivedColumnAliasing.IDENTIFICATION_VARIABLE_ONLY,
			ValuesTableRenderingStyle.VALUES,
			false,
			LateralPolicy.STANDARD,
			LateralPolicy.STANDARD,
			null,
			null
	);

	public static final DerivedTableRenderingSupport QUERY_SELECT_LIST = profile(
			DerivedColumnAliasing.SELECT_LIST,
			DerivedColumnAliasing.DECLARATION,
			DerivedColumnAliasing.IDENTIFICATION_VARIABLE_ONLY,
			ValuesTableRenderingStyle.VALUES,
			false,
			LateralPolicy.STANDARD,
			LateralPolicy.STANDARD,
			null,
			null
	);

	public static final DerivedTableRenderingSupport VALUES_SELECT_LIST = profile(
			DerivedColumnAliasing.DECLARATION,
			DerivedColumnAliasing.SELECT_LIST,
			DerivedColumnAliasing.IDENTIFICATION_VARIABLE_ONLY,
			ValuesTableRenderingStyle.SELECT_UNION,
			false,
			LateralPolicy.STANDARD,
			LateralPolicy.STANDARD,
			null,
			null
	);

	public static final DerivedTableRenderingSupport QUERY_AND_VALUES_SELECT_LIST = profile(
			DerivedColumnAliasing.SELECT_LIST,
			DerivedColumnAliasing.SELECT_LIST,
			DerivedColumnAliasing.IDENTIFICATION_VARIABLE_ONLY,
			ValuesTableRenderingStyle.SELECT_UNION,
			false,
			LateralPolicy.STANDARD,
			LateralPolicy.STANDARD,
			null,
			null
	);

	public static final DerivedTableRenderingSupport VALUES_SELECT_UNION = profile(
			DerivedColumnAliasing.DECLARATION,
			DerivedColumnAliasing.DECLARATION,
			DerivedColumnAliasing.IDENTIFICATION_VARIABLE_ONLY,
			ValuesTableRenderingStyle.SELECT_UNION,
			false,
			LateralPolicy.STANDARD,
			LateralPolicy.STANDARD,
			null,
			null
	);

	public static final DerivedTableRenderingSupport HANA = profile(
			DerivedColumnAliasing.SELECT_LIST,
			DerivedColumnAliasing.SELECT_LIST,
			DerivedColumnAliasing.IDENTIFICATION_VARIABLE_ONLY,
			ValuesTableRenderingStyle.SELECT_UNION,
			false,
			LateralPolicy.STANDARD,
			LateralPolicy.IMPLICIT,
			LateralPolicy.IMPLICIT,
			SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS,
			null
	);

	public static final DerivedTableRenderingSupport ORACLE = profile(
			DerivedColumnAliasing.SELECT_LIST,
			DerivedColumnAliasing.SELECT_LIST,
			DerivedColumnAliasing.IDENTIFICATION_VARIABLE_ONLY,
			ValuesTableRenderingStyle.SELECT_UNION,
			false,
			LateralPolicy.STANDARD,
			LateralPolicy.IMPLICIT,
			null,
			null
	);

	public static final DerivedTableRenderingSupport DB2 = profile(
			DerivedColumnAliasing.DECLARATION,
			DerivedColumnAliasing.DECLARATION,
			DerivedColumnAliasing.IDENTIFICATION_VARIABLE_ONLY,
			ValuesTableRenderingStyle.VALUES,
			false,
			LateralPolicy.STANDARD,
			LateralPolicy.IMPLICIT,
			null,
			null
	);

	public static final DerivedTableRenderingSupport DB2_ZOS = profile(
			DerivedColumnAliasing.DECLARATION,
			DerivedColumnAliasing.DECLARATION,
			DerivedColumnAliasing.IDENTIFICATION_VARIABLE_ONLY,
			ValuesTableRenderingStyle.VALUES,
			true,
			LateralPolicy.STANDARD,
			LateralPolicy.IMPLICIT,
			null,
			null
	);

	public static final DerivedTableRenderingSupport SQL_SERVER = profile(
			DerivedColumnAliasing.DECLARATION,
			DerivedColumnAliasing.DECLARATION,
			DerivedColumnAliasing.IDENTIFICATION_VARIABLE_ONLY,
			ValuesTableRenderingStyle.VALUES,
			false,
			LateralPolicy.IMPLICIT,
			LateralPolicy.IMPLICIT,
			null,
			null
	);

	public static final DerivedTableRenderingSupport FUNCTION_LATERAL_IMPLICIT = profile(
			DerivedColumnAliasing.DECLARATION,
			DerivedColumnAliasing.DECLARATION,
			DerivedColumnAliasing.IDENTIFICATION_VARIABLE_ONLY,
			ValuesTableRenderingStyle.VALUES,
			false,
			LateralPolicy.STANDARD,
			LateralPolicy.IMPLICIT,
			null,
			null
	);

	public static final DerivedTableRenderingSupport SPANNER = profile(
			DerivedColumnAliasing.SELECT_LIST,
			DerivedColumnAliasing.DECLARATION,
			DerivedColumnAliasing.IDENTIFICATION_VARIABLE_ONLY,
			ValuesTableRenderingStyle.VALUES,
			false,
			LateralPolicy.ARRAY_UNNEST,
			LateralPolicy.ARRAY_UNNEST,
			null,
			null
	);

	public static final DerivedTableRenderingSupport ALTIBASE = profile(
			DerivedColumnAliasing.SELECT_LIST,
			DerivedColumnAliasing.SELECT_LIST,
			DerivedColumnAliasing.IDENTIFICATION_VARIABLE_ONLY,
			ValuesTableRenderingStyle.SELECT_UNION,
			false,
			LateralPolicy.STANDARD,
			LateralPolicy.STANDARD,
			null,
			SqlAstNodeRenderingMode.INLINE_PARAMETERS
	);

	public static final DerivedTableRenderingSupport MYSQL_BEFORE_8 = mysql( false );
	public static final DerivedTableRenderingSupport MYSQL_8 = mysql( true );

	private final DerivedColumnAliasing queryPartAliasing;
	private final DerivedColumnAliasing valuesAliasing;
	private final DerivedColumnAliasing functionAliasing;
	private final ValuesTableRenderingStyle valuesRenderingStyle;
	private final boolean tablePrefix;
	private final LateralPolicy queryPartLateralPolicy;
	private final LateralPolicy inlineCteLateralPolicy;
	private final LateralPolicy functionLateralPolicy;
	private final SqlAstNodeRenderingMode lateralQueryRenderingMode;
	private final SqlAstNodeRenderingMode valuesRenderingMode;

	private StandardDerivedTableRenderingSupport(
			DerivedColumnAliasing queryPartAliasing,
			DerivedColumnAliasing valuesAliasing,
			DerivedColumnAliasing functionAliasing,
			ValuesTableRenderingStyle valuesRenderingStyle,
			boolean tablePrefix,
			LateralPolicy queryPartLateralPolicy,
			LateralPolicy inlineCteLateralPolicy,
			LateralPolicy functionLateralPolicy,
			@Nullable SqlAstNodeRenderingMode lateralQueryRenderingMode,
			@Nullable SqlAstNodeRenderingMode valuesRenderingMode) {
		this.queryPartAliasing = Objects.requireNonNull( queryPartAliasing, "queryPartAliasing" );
		this.valuesAliasing = Objects.requireNonNull( valuesAliasing, "valuesAliasing" );
		this.functionAliasing = Objects.requireNonNull( functionAliasing, "functionAliasing" );
		this.valuesRenderingStyle = Objects.requireNonNull( valuesRenderingStyle, "valuesRenderingStyle" );
		this.tablePrefix = tablePrefix;
		this.queryPartLateralPolicy = Objects.requireNonNull( queryPartLateralPolicy, "queryPartLateralPolicy" );
		this.inlineCteLateralPolicy = Objects.requireNonNull( inlineCteLateralPolicy, "inlineCteLateralPolicy" );
		this.functionLateralPolicy = Objects.requireNonNull( functionLateralPolicy, "functionLateralPolicy" );
		this.lateralQueryRenderingMode = lateralQueryRenderingMode;
		this.valuesRenderingMode = valuesRenderingMode;
	}

	@Override
	public DerivedTableRenderingPlan determinePlan(DerivedTableRenderingRequest request) {
		Objects.requireNonNull( request, "request" );
		return switch ( request.kind() ) {
			case QUERY_PART -> new DerivedTableRenderingPlan.QueryPart(
					queryPartAliasing,
					determineLateralStyle( request, queryPartLateralPolicy ),
					tablePrefix,
					request.lateral() ? lateralQueryRenderingMode : null
			);
			case INLINE_CTE -> new DerivedTableRenderingPlan.QueryPart(
					queryPartAliasing,
					determineLateralStyle( request, inlineCteLateralPolicy ),
					tablePrefix,
					request.lateral() ? lateralQueryRenderingMode : null
			);
			case VALUES -> new DerivedTableRenderingPlan.Values(
					valuesAliasing,
					valuesRenderingStyle,
					valuesRenderingMode
			);
			case FUNCTION -> new DerivedTableRenderingPlan.Function(
					functionAliasing,
					determineLateralStyle( request, functionLateralPolicy ),
					null
			);
		};
	}

	private static DerivedTableRenderingSupport mysql(boolean supportsQueryPartColumnList) {
		return profile(
				supportsQueryPartColumnList
						? DerivedColumnAliasing.DECLARATION
						: DerivedColumnAliasing.SELECT_LIST,
				DerivedColumnAliasing.SELECT_LIST,
				DerivedColumnAliasing.IDENTIFICATION_VARIABLE_ONLY,
				ValuesTableRenderingStyle.SELECT_UNION,
				false,
				LateralPolicy.STANDARD,
				LateralPolicy.IMPLICIT,
				null,
				null
		);
	}

	private static DerivedTableRenderingSupport profile(
			DerivedColumnAliasing queryPartAliasing,
			DerivedColumnAliasing valuesAliasing,
			DerivedColumnAliasing functionAliasing,
			ValuesTableRenderingStyle valuesRenderingStyle,
			boolean tablePrefix,
			LateralPolicy queryPartLateralPolicy,
			LateralPolicy functionLateralPolicy,
			@Nullable SqlAstNodeRenderingMode lateralQueryRenderingMode,
			@Nullable SqlAstNodeRenderingMode valuesRenderingMode) {
		return profile(
				queryPartAliasing,
				valuesAliasing,
				functionAliasing,
				valuesRenderingStyle,
				tablePrefix,
				queryPartLateralPolicy,
				queryPartLateralPolicy,
				functionLateralPolicy,
				lateralQueryRenderingMode,
				valuesRenderingMode
		);
	}

	private static DerivedTableRenderingSupport profile(
			DerivedColumnAliasing queryPartAliasing,
			DerivedColumnAliasing valuesAliasing,
			DerivedColumnAliasing functionAliasing,
			ValuesTableRenderingStyle valuesRenderingStyle,
			boolean tablePrefix,
			LateralPolicy queryPartLateralPolicy,
			LateralPolicy inlineCteLateralPolicy,
			LateralPolicy functionLateralPolicy,
			@Nullable SqlAstNodeRenderingMode lateralQueryRenderingMode,
			@Nullable SqlAstNodeRenderingMode valuesRenderingMode) {
		return new StandardDerivedTableRenderingSupport(
				queryPartAliasing,
				valuesAliasing,
				functionAliasing,
				valuesRenderingStyle,
				tablePrefix,
				queryPartLateralPolicy,
				inlineCteLateralPolicy,
				functionLateralPolicy,
				lateralQueryRenderingMode,
				valuesRenderingMode
		);
	}

	private static LateralReferenceStyle determineLateralStyle(
			DerivedTableRenderingRequest request,
			LateralPolicy policy) {
		if ( !request.lateral() ) {
			return LateralReferenceStyle.IMPLICIT;
		}
		return switch ( policy ) {
			case STANDARD -> request.supportsLateralKeyword()
					? LateralReferenceStyle.KEYWORD
					: request.kind() == DerivedTableKind.QUERY_PART
							|| request.kind() == DerivedTableKind.INLINE_CTE
							? LateralReferenceStyle.EMULATED_QUERY_PART
							: LateralReferenceStyle.IMPLICIT;
			case IMPLICIT -> LateralReferenceStyle.IMPLICIT;
			case ARRAY_UNNEST -> LateralReferenceStyle.ARRAY_UNNEST;
		};
	}

	private enum LateralPolicy {
		STANDARD,
		IMPLICIT,
		ARRAY_UNNEST
	}
}
