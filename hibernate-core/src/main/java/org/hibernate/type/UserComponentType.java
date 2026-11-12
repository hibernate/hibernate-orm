package org.hibernate.type;

import org.hibernate.Internal;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.internal.ContextualTemporalCompositeUserType;
import org.hibernate.type.internal.TemporalPreparedState;

import java.io.Serializable;
import java.util.Map;

/**
 * Handles {@link CompositeUserType}s.
 *
 * @author Gavin King
 *
 * @since 6.3
 */
@Internal
public class UserComponentType<T> extends ComponentType {

	private final CompositeUserType<T> compositeUserType;

	public UserComponentType(
			Component component,
			int[] originalPropertyOrder,
			CompositeUserType<T> compositeUserType) {
		super( component, originalPropertyOrder, compositeUserType.isMutable() );
		this.compositeUserType = compositeUserType;
	}

	public CompositeUserType<T> getCompositeUserType() {
		return compositeUserType;
	}

	@Override
	public boolean isDirty(Object old, Object current, SharedSessionContractImplementor session) {
		return compositeUserType instanceof ContextualTemporalCompositeUserType<?> temporal && temporal.hasPrecision()
				? !temporal.precisionEqual( old, current ) : super.isDirty( old, current, session );
	}

	@Override
	public boolean isDirty(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session) {
		if ( compositeUserType instanceof ContextualTemporalCompositeUserType<?> temporal && temporal.hasPrecision() ) {
			for ( boolean included : checkable ) {
				if ( included ) { return !temporal.precisionEqual( old, current ); }
			}
			return false;
		}
		return super.isDirty( old, current, checkable, session );
	}

	@Override
	public boolean isModified(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session) {
		return compositeUserType instanceof ContextualTemporalCompositeUserType<?> temporal && temporal.hasPrecision()
				? isDirty( old, current, checkable, session ) : super.isModified( old, current, checkable, session );
	}

	@Override
	public boolean isEqual(Object x, Object y) throws HibernateException {
		return x==y || compositeUserType.equals( (T) x, (T) y );
	}

	@Override
	public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory)
			throws HibernateException {
		return isEqual( x, y );
	}

	@Override
	public int getHashCode(Object x) {
		return compositeUserType.hashCode( (T) x );
	}

	@Override
	public int getHashCode(Object x, SessionFactoryImplementor factory) {
		return getHashCode( x );
	}

	@Override
	public Object deepCopy(Object component, SessionFactoryImplementor factory) {
		return component == null ? null : compositeUserType.deepCopy( (T) component );
	}

	@Override
	public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner, Map<Object, Object> copyCache) {
		return original == null || !isMutable() ? original : compositeUserType.replace( (T) original, (T) target, owner );
	}

	@Override
	public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner, Map<Object, Object> copyCache, ForeignKeyDirection foreignKeyDirection) {
		return replace( original, target, session, owner, copyCache );
	}

	@Override
	public Serializable disassemble(Object value, SessionFactoryImplementor sessionFactory)
			throws HibernateException {
		return value == null ? null : compositeUserType.disassemble( (T) value );
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		final var state = compositeUserType instanceof ContextualTemporalCompositeUserType<?> temporal && temporal.hasPrecision()
				? TemporalPreparedState.forEntity( owner, session ) : null;
		return state != null && state.contains( compositeUserType )
				? state.get( compositeUserType ) : disassemble( value, session.getFactory() );
	}

	@Override
	public Object assemble(Serializable object, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		final Object assembled = object == null ? null : compositeUserType.assemble( object, owner );
		if ( compositeUserType instanceof ContextualTemporalCompositeUserType<?> temporal && temporal.hasPrecision() ) {
			final var state = TemporalPreparedState.forEntity( owner, session );
			if ( state != null ) { state.recordCacheValue( temporal, object ); }
		}
		return assembled;
	}

	@Override
	public Object replacePropertyValues(Object component, Object[] values, SharedSessionContractImplementor session)
			throws HibernateException {
		return instantiator( component ).instantiate( () -> values );
	}
}
