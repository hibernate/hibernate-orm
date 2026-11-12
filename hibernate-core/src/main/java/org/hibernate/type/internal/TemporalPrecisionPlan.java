package org.hibernate.type.internal;

import java.util.Objects;

import org.hibernate.TemporalValueException;
import org.hibernate.dialect.temporaltype.internal.TemporalPrecisionHelper;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;
import org.hibernate.type.descriptor.java.JavaType;

/// Mapping-local precision operations on relational values. Application conversion
/// is coordinated by the caller, never repeated by this plan. Preparation is an
/// assignment operation; comparison parameters must not call it implicitly.
///
/// The plan contains no mutable session state and does not execute JDBC. Bindings
/// remain owned by the resolved JdbcType. Identity-valued mappings do not install
/// this plan, and version preparation does not test for token collisions.
///
/// @author Steve Ebersole
public final class TemporalPrecisionPlan {
	private final TemporalPrecisionContext context;
	private final TemporalPrecisionSupport support;
	private final String signature;

	public TemporalPrecisionPlan(TemporalPrecisionContext context, TemporalPrecisionSupport support) {
		this.context = Objects.requireNonNull( context );
		this.support = Objects.requireNonNull( support );
		if ( !support.isSupported() ) {
			throw TemporalPrecisionHelper.failure( context, support.getUnsupportedReason(), null );
		}
		signature = "temporal-precision-v1|" + support.getSignature()
				+ '|' + context.javaType().getClass().getName() + '|' + context.jdbcType().getClass().getName()
				+ '|' + context.ddlTypeCode() + '|' + context.ddlTypeName()
				+ '|' + context.declaredPrecision() + '|' + context.precision()
				+ '|' + context.length() + '|' + context.scale()
				+ '|' + context.policy() + '|' + context.representation() + '|' + context.adjustment();
	}

	public TemporalPrecisionContext getContext() {
		return context;
	}

	public String getSignature() {
		return signature;
	}

	/// Reduces assignment precision without applying read-representation changes.
	public Object prepare(Object relationalValue) {
		if ( relationalValue == null ) {
			return null;
		}
		try {
			checkType( relationalValue );
			final Object prepared = support.quantize( relationalValue );
			checkType( prepared );
			return prepared;
		}
		catch (RuntimeException exception) {
			throw valueFailure( "cannot prepare value: " + exception.getMessage(), exception );
		}
	}

	/// Uses an already prepared JDBC value, avoiding another application conversion.
	public Object reconstructPrepared(Object preparedValue) {
		if ( preparedValue == null ) {
			return null;
		}
		try {
			checkType( preparedValue );
			final Object reconstructed = support.reconstruct( preparedValue );
			checkType( reconstructed );
			validateExtracted( reconstructed );
			return reconstructed;
		}
		catch (RuntimeException exception) {
			throw valueFailure( "cannot reconstruct prepared value: " + exception.getMessage(), exception );
		}
	}

	/// Validates rather than repairs a value returned by JDBC. Unexpected reduction
	/// cannot be detected by grid membership alone; provider fixed-point regressions
	/// independently verify that writes survive the complete route.
	public void validateExtracted(Object relationalValue) {
		if ( relationalValue == null ) {
			return;
		}
		checkType( relationalValue );
		if ( !equal( relationalValue, prepare( relationalValue ) ) ) {
			throw valueFailure( "extracted value is not representable by the resolved precision contract: "
					+ relationalValue, null );
		}
	}

	/// Precision-only comparison deliberately does not apply the read representation,
	/// so policy normalization cannot hide an offset-only assignment change.
	public boolean precisionEqual(Object first, Object second) {
		return equal( prepare( first ), prepare( second ) );
	}

	@SuppressWarnings("unchecked")
	private boolean equal(Object first, Object second) {
		return ((JavaType<Object>) context.javaType()).areEqual( first, second );
	}

	private void checkType(Object value) {
		if ( value == null || !context.javaType().isInstance( value ) ) {
			throw valueFailure( "precision contribution returned/received an incompatible relational value", null );
		}
	}

	private TemporalValueException valueFailure(String message, Throwable cause) {
		return new TemporalValueException( "Temporal precision for '" + context.role() + "' ("
				+ context.operation() + ", " + context.ddlTypeName() + ", " + support.getSignature()
				+ "): " + message, cause );
	}
}
