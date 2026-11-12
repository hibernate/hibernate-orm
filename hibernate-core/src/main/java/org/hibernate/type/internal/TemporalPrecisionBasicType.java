package org.hibernate.type.internal;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.AdjustableBasicType;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.CompositeType;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/// Mapping-local precision lifecycle adapter. JDBC binders and ordinary relational
/// conversion remain unchanged, so a query comparison does not accidentally use
/// the assignment grid. Mutation preparation explicitly invokes [#prepareRelational].
/// Cache assembly uses the same read reconstruction as the resolved plan.
///
/// @author Steve Ebersole
public final class TemporalPrecisionBasicType<T> extends AbstractDelegatingBasicType<T> {
	private final TemporalPrecisionPlan precision;

	public TemporalPrecisionBasicType(BasicType<T> delegate, TemporalPrecisionPlan precision) {
		super( delegate );
		this.precision = precision;
	}

	public TemporalPrecisionPlan getPrecisionPlan() {
		return precision;
	}

	/// Finds the mapping adapter through policy wrappers, without inspecting class names.
	public static TemporalPrecisionBasicType<?> find(JdbcMapping mapping) {
		while ( mapping instanceof AbstractDelegatingBasicType<?> wrapper ) {
			if ( wrapper instanceof TemporalPrecisionBasicType<?> temporal ) {
				return temporal;
			}
			mapping = wrapper.getDelegate();
		}
		return null;
	}

	/// Period endpoints may own separate immutable adapters for the same execution
	/// contract. Other mappings retain the existing descriptor identity requirement.
	public static boolean sameJdbcContract(JdbcMapping first, JdbcMapping second) {
		if ( first == second ) {
			return true;
		}
		final var left = find( first );
		final var right = find( second );
		return left != null && right != null && left.getDelegate() == right.getDelegate()
				&& left.precision.getSignature().equals( right.precision.getSignature() );
	}

	/// Mutation-only binder selection. Restriction parameters retain their original
	/// comparison semantics; delayed mutation values are resolved by the caller.
	@SuppressWarnings("unchecked")
	public static ValueBinder<Object> mutationBinder(JdbcMapping mapping, ParameterUsage usage) {
		final ValueBinder<Object> binder = mapping.getJdbcValueBinder();
		final var temporal = usage == ParameterUsage.SET ? find( mapping ) : null;
		if ( temporal == null ) {
			return binder;
		}
		return new ValueBinder<>() {
			@Override
			public void bind(PreparedStatement statement, Object value, int index, WrapperOptions options) throws SQLException {
				binder.bind( statement, temporal.prepareRelational( value ), index, options );
			}

			@Override
			public void bind(CallableStatement statement, Object value, String name, WrapperOptions options) throws SQLException {
				binder.bind( statement, temporal.prepareRelational( value ), name, options );
			}

			@Override
			public Object getBindValue(Object value, WrapperOptions options) throws SQLException {
				return binder.getBindValue( temporal.prepareRelational( value ), options );
			}
		};
	}

	/// Whether default enhanced dirty tracking must refine a candidate property.
	public static boolean contains(Type type) {
		if ( type instanceof BasicType<?> basic ) {
			return find( basic ) != null;
		}
		if ( type instanceof CompositeType composite ) {
			for ( Type subtype : composite.getSubtypes() ) {
				if ( contains( subtype ) ) {
					return true;
				}
			}
		}
		return false;
	}

	public Object prepareRelational(Object relationalValue) {
		return precision.prepare( relationalValue );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> BasicType<X> resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<X> javaType) {
		if ( delegate instanceof AdjustableBasicType<?> adjustable
				&& adjustable.resolveIndicatedType( indicators, javaType ) != delegate ) {
			throw new MappingException( "Temporal precision mapping '" + precision.getContext().role()
					+ "' must be resolved again after changing its JDBC type" );
		}
		return (BasicType<X>) this;
	}

	@Override
	public boolean isDirty(Object old, Object current, SharedSessionContractImplementor session) {
		return getValueConverter() instanceof JpaAttributeConverter<?, ?> || delegate.isMutable()
				? delegate.isDirty( old, current, session )
				: !precision.precisionEqual( old, current );
	}

	@Override
	public boolean isDirty(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session) {
		return checkable[0] && isDirty( old, current, session );
	}

	@Override
	public boolean isModified(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session) {
		return isDirty( old, current, checkable, session );
	}

	@Override
	public ValueExtractor<T> getJdbcValueExtractor() {
		final var extractor = delegate.getJdbcValueExtractor();
		return new ValueExtractor<>() {
			private T checked(T value) {
				precision.validateExtracted( value );
				return value;
			}

			@Override
			public T extract(ResultSet rows, int index, WrapperOptions options) throws SQLException {
				return checked( extractor.extract( rows, index, options ) );
			}

			@Override
			public T extract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return checked( extractor.extract( statement, index, options ) );
			}

			@Override
			public T extract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return checked( extractor.extract( statement, name, options ) );
			}
		};
	}

	@Override
	public Serializable disassemble(Object value, SessionFactoryImplementor factory) {
		return disassemble( value, null, null );
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner) {
		if ( owner != null && session != null ) {
			final var entry = session.getPersistenceContextInternal().getEntry( owner );
			final var state = entry == null ? null : entry.getExtraState( TemporalPreparedState.class );
			if ( state != null && state.contains( this ) ) {
				return state.get( this );
			}
		}
		if ( getValueConverter() instanceof JpaAttributeConverter<?, ?> ) {
			throw missingPreparedState( "entity attribute" );
		}
		if ( value == null ) {
			return null;
		}
		final Object prepared = precision.prepare( delegate.convertToRelationalValue( value ) );
		return disassemblePrepared( prepared, session );
	}

	public MappingException missingPreparedState(String occurrence) {
		return new MappingException( "Missing prepared relational state for '" + precision.getContext().role()
				+ "' during cache disassembly (" + occurrence + "); the actual bound, extracted or cache-assembled"
				+ " JDBC value must be retained; an application converter cannot be invoked again to recreate it" );
	}

	/// Uses the actual bound/read relational value without repeating application conversion.
	public Serializable disassemblePrepared(Object prepared, SharedSessionContractImplementor session) {
		return new CacheValue( precision.getSignature(),
				relationalMutabilityPlan().disassemble( prepared, session ) );
	}

	@Override
	public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner) {
		return assemble( cached, session, owner, null );
	}

	public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner,
			TemporalPreparedState state) {
		if ( cached == null && !(getValueConverter() instanceof JpaAttributeConverter<?, ?>) ) {
			return null;
		}
		if ( !(cached instanceof CacheValue value) || !precision.getSignature().equals( value.signature() ) ) {
			throw new MappingException( "Incompatible temporal cache payload for '" + precision.getContext().role()
					+ "'; evict affected cache regions before changing temporal precision or representation" );
		}
		final var relational = relationalMutabilityPlan().assemble( value.relational(), session );
		if ( state != null ) {
			state.record( this, relational, session );
		}
		if ( state == null && owner != null && session != null ) {
			TemporalPreparedState.record( session.getPersistenceContextInternal().getEntry( owner ), this, relational, session );
		}
		return delegate.convertToDomainValue( precision.reconstructPrepared( relational ) );
	}

	@SuppressWarnings("unchecked")
	private MutabilityPlan<Object> relationalMutabilityPlan() {
		return (MutabilityPlan<Object>) getJdbcJavaType().getMutabilityPlan();
	}

	private record CacheValue(String signature, Serializable relational) implements Serializable {
	}
}
