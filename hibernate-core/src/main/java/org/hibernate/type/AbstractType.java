/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/**
 * Abstract superclass of the built-in {@link Type} hierarchy.
 *
 * @author Gavin King
 */
@SPI({ USE, IMPLEMENT })
public abstract class AbstractType implements Type {
	@SPI(IMPLEMENT)
	public AbstractType() {
	}


	@Override
	public boolean isAssociationType() {
		return false;
	}

	@Override
	public boolean isCollectionType() {
		return false;
	}

	@Override
	public boolean isComponentType() {
		return false;
	}

	@Override
	public boolean isEntityType() {
		return false;
	}

	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	public int compare(@Nullable Object x, @Nullable Object y) {
		return ( (Comparable) x ).compareTo(y);
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		return value == null ? null : (Serializable) deepCopy( value, session.getFactory() );
	}

	@Override
	public Serializable disassemble(Object value, SessionFactoryImplementor sessionFactory)
			throws HibernateException {
		return value == null ? null : (Serializable) deepCopy( value, sessionFactory );
	}

	@Override
	public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner)
	throws HibernateException {
		return cached == null ? null : deepCopy( cached, session.getFactory() );
	}

	@Override
	public boolean isDirty(@Nullable Object old, @Nullable Object current, SharedSessionContractImplementor session)
			throws HibernateException {
		return !isSame( old, current );
	}

	@Override
	public boolean isAnyType() {
		return false;
	}

	@Override
	public boolean isModified(@Nullable Object old, @Nullable Object current, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException {
		return isDirty( old, current, session );
	}

	@Override
	public boolean isSame(@Nullable Object x, @Nullable Object y) throws HibernateException {
		return isEqual(x, y );
	}

	@Override
	public boolean isEqual(@Nullable Object x, @Nullable Object y) {
		return Objects.equals( x, y );
	}

	@Override
	public int getHashCode(Object x) {
		return x.hashCode();
	}

	@Override
	public boolean isEqual(@Nullable Object x, @Nullable Object y, SessionFactoryImplementor factory) {
		return isEqual( x, y );
	}

	@Override
	public int getHashCode(Object x, SessionFactoryImplementor factory) {
		return getHashCode( x );
	}

	@Override
	@Nullable
	public Object replace(
			@Nullable Object original,
			@Nullable Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map<Object, Object> copyCache,
			ForeignKeyDirection foreignKeyDirection)
	throws HibernateException {
		return needsReplacement( foreignKeyDirection ) ? replace( original, target, session, owner, copyCache ) : target;
	}

	private boolean needsReplacement(ForeignKeyDirection foreignKeyDirection) {
		// Collection and OneToOne are the only associations that could be TO_PARENT
		if ( this instanceof CollectionType || this instanceof OneToOneType ) {
			final var associationType = (AssociationType) this;
			return associationType.getForeignKeyDirection() == foreignKeyDirection;
		}
		else {
			return ForeignKeyDirection.FROM_PARENT == foreignKeyDirection;
		}
	}

}
