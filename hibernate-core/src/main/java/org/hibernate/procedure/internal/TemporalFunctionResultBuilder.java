package org.hibernate.procedure.internal;

import jakarta.persistence.ParameterMode;

import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext.Access;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.results.internal.dynamic.DynamicResultBuilderBasicStandard;
import org.hibernate.type.BasicType;
import org.hibernate.type.internal.TemporalCallableType;
import org.hibernate.type.internal.TemporalMappingValidator;

/// Immutable marker for an automatically inferred scalar function result. Resolve
/// the actual ResultSet route only after the Dialect has interpreted the call.
/// Mementos retain the declared type, never a session or execution value.
///
/// @author Steve Ebersole
final class TemporalFunctionResultBuilder extends DynamicResultBuilderBasicStandard {
	private final BasicType<?> type;

	TemporalFunctionResultBuilder(BasicType<?> type) { super( 1, type ); this.type = type; }

	DynamicResultBuilderBasicStandard resolve(SharedSessionContractImplementor session, String role) {
		if ( session.getFactory().getSessionFactoryOptions().isPreferJavaTimeJdbcTypesEnabled() ) {
			TemporalMappingValidator.validate( session.getJdbcServices().getDialect(), type, role, session.getFactory().getProperties() );
		}
		return new DynamicResultBuilderBasicStandard( 1, (BasicType<?>) TemporalCallableType.resolve(
				type, session, role, ParameterMode.OUT, true, Access.RESULT_SET ) );
	}
}
