/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.Objects;

import static org.hibernate.dialect.sql.ast.spi.SetReturningFunctionRenderingPlan.DerivedOrdinality.InvocationWrapper.NONE;
import static org.hibernate.dialect.sql.ast.spi.SetReturningFunctionRenderingPlan.DerivedOrdinality.InvocationWrapper.TABLE;
import static org.hibernate.dialect.sql.ast.spi.SetReturningFunctionRenderingPlan.DerivedOrdinality.OrdinalityExpression.ROWNUM;
import static org.hibernate.dialect.sql.ast.spi.SetReturningFunctionRenderingPlan.DerivedOrdinality.OrdinalityExpression.ROW_NUMBER;
import static org.hibernate.dialect.sql.ast.spi.SetReturningFunctionRenderingPlan.DerivedOrdinality.OrdinalityExpression.ROW_NUMBER_DUMMY_ORDER;
import static org.hibernate.dialect.sql.ast.spi.SetReturningFunctionRenderingPlan.Native.Ordinality.WITH_ORDINALITY;

/// Standard and family named set-returning-function rendering strategies.
///
/// @since 8.0
/// @author Steve Ebersole
public final class StandardSetReturningFunctionRenderingSupport {
	private static final SetReturningFunctionRenderingPlan NATIVE_PLAN =
			new SetReturningFunctionRenderingPlan.Native(
					SetReturningFunctionRenderingPlan.Native.Ordinality.NONE
			);
	private static final SetReturningFunctionRenderingPlan NATIVE_WITH_ORDINALITY_PLAN =
			new SetReturningFunctionRenderingPlan.Native( WITH_ORDINALITY );
	private static final SetReturningFunctionRenderingPlan TABLE_WRAPPED =
			new SetReturningFunctionRenderingPlan.TableWrapped();
	private static final SetReturningFunctionRenderingPlan UNSUPPORTED =
			new SetReturningFunctionRenderingPlan.Unsupported();
	private static final SetReturningFunctionRenderingPlan HANA_ORDINALITY =
			new SetReturningFunctionRenderingPlan.DerivedOrdinality( NONE, ROW_NUMBER, false );
	private static final SetReturningFunctionRenderingPlan SQL_SERVER_ORDINALITY =
			new SetReturningFunctionRenderingPlan.DerivedOrdinality( NONE, ROW_NUMBER_DUMMY_ORDER, false );
	private static final SetReturningFunctionRenderingPlan DB2_ORDINALITY =
			new SetReturningFunctionRenderingPlan.DerivedOrdinality( TABLE, ROW_NUMBER, true );
	private static final SetReturningFunctionRenderingPlan ORACLE_ORDINALITY =
			new SetReturningFunctionRenderingPlan.DerivedOrdinality( TABLE, ROWNUM, true );

	/// Native invocation without native ordinality support.
	public static final SetReturningFunctionRenderingSupport NATIVE = request -> {
		Objects.requireNonNull( request, "request" );
		return request.ordinalityRequested() ? UNSUPPORTED : NATIVE_PLAN;
	};

	/// Native invocation with `with ordinality` support.
	public static final SetReturningFunctionRenderingSupport NATIVE_WITH_ORDINALITY = request -> {
		Objects.requireNonNull( request, "request" );
		return request.ordinalityRequested()
				? NATIVE_WITH_ORDINALITY_PLAN
				: NATIVE_PLAN;
	};

	public static final SetReturningFunctionRenderingSupport HANA = request ->
			ordinalityPlan( request, HANA_ORDINALITY, NATIVE_PLAN );

	public static final SetReturningFunctionRenderingSupport SQL_SERVER = request ->
			ordinalityPlan( request, SQL_SERVER_ORDINALITY, NATIVE_PLAN );

	public static final SetReturningFunctionRenderingSupport DB2 = request ->
			ordinalityPlan( request, DB2_ORDINALITY, TABLE_WRAPPED );

	public static final SetReturningFunctionRenderingSupport ORACLE = request ->
			ordinalityPlan( request, ORACLE_ORDINALITY, TABLE_WRAPPED );

	private static SetReturningFunctionRenderingPlan ordinalityPlan(
			SetReturningFunctionRenderingRequest request,
			SetReturningFunctionRenderingPlan ordinalityPlan,
			SetReturningFunctionRenderingPlan ordinaryPlan) {
		Objects.requireNonNull( request, "request" );
		return request.ordinalityRequested() ? ordinalityPlan : ordinaryPlan;
	}

	private StandardSetReturningFunctionRenderingSupport() {
	}
}
