package org.hibernate.type.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.temporal.Temporal;

import jakarta.persistence.ParameterMode;

import org.hibernate.TemporalValueException;
import org.hibernate.MappingException;
import org.hibernate.dialect.temporaltype.internal.TemporalCallablePrecisionHelper;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext.Access;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext.Boundary;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.BasicType;
import org.hibernate.type.OutputableType;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.converter.internal.TemporalAdjustmentConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JavaTimeJdbcType;

/// Immutable callable view of a scalar type. Attribute precision is isolated,
/// while conversions and the executing JDBC descriptor remain authoritative.
/// Values are validated, never rounded, and never retained in this type.
///
/// @author Steve Ebersole
public final class TemporalCallableType<T> extends AbstractDelegatingBasicType<T> {
	private final Plan input;
	private final Plan output;

	private TemporalCallableType(BasicType<T> delegate, Plan input, Plan output) {
		super( delegate );
		this.input = input;
		this.output = output;
	}

	public static <T> OutputableType<T> resolve(OutputableType<T> original, SharedSessionContractImplementor session,
			String role, ParameterMode mode, boolean functionReturn, Access outputAccess) {
		if ( !session.getFactory().getSessionFactoryOptions().isPreferJavaTimeJdbcTypesEnabled() ) {
			return original;
		}
		if ( !(original instanceof BasicType<T> basic) ) {
			if ( original != null && Temporal.class.isAssignableFrom( original.getJavaType() ) ) {
				throw new MappingException( "Callable temporal precision for '" + role + "' (" + mode
						+ "): opaque OutputableType " + original.getClass().getName()
						+ " does not expose a scalar relational validation boundary; use an evidenced BasicType" );
			}
			return original;
		}
		if ( !TemporalMappingValidator.isTemporal( basic.getJdbcJavaType() ) ) {
			return original;
		}
		final var type = isolate( basic );
		final Plan input = mode == ParameterMode.IN || mode == ParameterMode.INOUT
				? plan( type, session, role, mode, functionReturn, Boundary.INPUT, Access.INDEXED ) : null;
		final Plan output = mode == ParameterMode.OUT || mode == ParameterMode.INOUT
				? plan( type, session, role, mode, functionReturn, Boundary.OUTPUT, outputAccess ) : null;
		if ( output != null ) {
			checkExtraction( type, output.context );
		}
		return new TemporalCallableType<>( type, input, output );
	}

	private static <T> BasicType<T> isolate(BasicType<T> type) {
		if ( type instanceof TemporalPrecisionBasicType<T> precision ) {
			return isolate( precision.getDelegate() );
		}
		if ( type instanceof TemporalPolicyBasicType<T> policy ) {
			return new TemporalPolicyBasicType<>( isolate( policy.getDelegate() ), policy.getPolicy() );
		}
		return type;
	}

	private static Plan plan(BasicType<?> type, SharedSessionContractImplementor session, String role,
			ParameterMode mode, boolean function, Boundary boundary, Access access) {
		final var jdbc = type.getJdbcType();
		final var dialect = session.getJdbcServices().getDialect();
		final var adjustment = type.getValueConverter() instanceof TemporalAdjustmentConverter<?> converter ? converter.getTarget() : null;
		final var policy = type instanceof TemporalPolicyBasicType<?> mapped ? mapped.getPolicy()
				: TemporalPolicySettings.resolve( session.getFactory().getProperties(), type.getJdbcJavaType().getJavaTypeClass() );
		final var context = new TemporalCallablePrecisionContext( type, dialect,
				jdbc instanceof JavaTimeJdbcType ? jdbc.getDdlTypeCode() : jdbc.getJdbcTypeCode(), mode, boundary, access,
				boundary == Boundary.INPUT || access == Access.RESULT_SET ? Access.NONE : Access.INDEXED,
				function, role, policy.policy(), adjustment );
		final var support = dialect.getTemporalCallablePrecisionSupport( context );
		if ( support == null ) {
			throw TemporalCallablePrecisionHelper.failure( context, "Dialect returned null support" );
		}
		if ( !support.isSupported() ) {
			throw TemporalCallablePrecisionHelper.failure( context, support.getUnsupportedReason() );
		}
		return new Plan( context, support );
	}

	private static void checkExtraction(BasicType<?> type, TemporalCallablePrecisionContext context) {
		if ( type instanceof TemporalPolicyBasicType<?> policy ) {
			checkExtraction( policy.getDelegate(), context );
			return;
		}
		try {
			final var owner = type.getClass().getMethod( "extract", CallableStatement.class, context.valueAccess() == Access.NAMED ? String.class : int.class,
					SharedSessionContractImplementor.class ).getDeclaringClass();
			if ( owner != AbstractStandardBasicType.class && owner != ConvertedBasicTypeImpl.class ) {
				throw TemporalCallablePrecisionHelper.failure( context, "Custom callable extraction cannot be separated safely from relational validation" );
			}
		}
		catch (NoSuchMethodException failure) {
			throw TemporalCallablePrecisionHelper.failure( context, "Missing scalar callable extraction contract" );
		}
	}

	@Override
	public <X> Object convertToRelationalValue(X value) {
		final var relational = delegate.convertToRelationalValue( value );
		if ( input != null ) {
			input.validate( relational );
		}
		return relational;
	}

	@Override
	public ValueExtractor<T> getJdbcValueExtractor() {
		final var extractor = delegate.getJdbcValueExtractor();
		return new ValueExtractor<>() {
			private T checked(T value) { if ( output != null ) { output.validate( value ); } return value; }
			@Override public T extract(ResultSet rows, int index, WrapperOptions options) throws SQLException { return checked( extractor.extract( rows, index, options ) ); }
			@Override public T extract(CallableStatement call, int index, WrapperOptions options) throws SQLException { return checked( extractor.extract( call, index, options ) ); }
			@Override public T extract(CallableStatement call, String name, WrapperOptions options) throws SQLException { return checked( extractor.extract( call, name, options ) ); }
		};
	}

	@Override
	public T extract(CallableStatement statement, int index, SharedSessionContractImplementor session) throws SQLException {
		return domain( getJdbcValueExtractor().extract( statement, index, session ) );
	}

	@Override
	public T extract(CallableStatement statement, String name, SharedSessionContractImplementor session) throws SQLException {
		return domain( getJdbcValueExtractor().extract( statement, name, session ) );
	}

	@SuppressWarnings("unchecked")
	private T domain(Object relational) { return (T) delegate.convertToDomainValue( relational ); }

	private record Plan(TemporalCallablePrecisionContext context, TemporalPrecisionSupport support) {
		@SuppressWarnings({ "rawtypes", "unchecked" })
		void validate(Object value) {
			if ( value == null ) { return; }
			final JavaType java = context.type().getJdbcJavaType();
			boolean valid;
			try {
				final Object candidate = support.quantize( value );
				valid = java.isInstance( value ) && candidate != null && java.isInstance( candidate ) && java.areEqual( value, candidate );
			}
			catch (RuntimeException failure) {
				throw new TemporalValueException( TemporalCallablePrecisionHelper.describe( context ) + ": validation failed for contract " + support.getSignature(), failure );
			}
			if ( !valid ) {
				throw new TemporalValueException( TemporalCallablePrecisionHelper.describe( context )
						+ ": value is not representable without changing precision; contract=" + support.getSignature() );
			}
		}
	}
}
