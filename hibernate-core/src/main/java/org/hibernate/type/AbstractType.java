/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Abstract superclass of the built-in {@link Type} hierarchy.
 *
 * @author Gavin King
 */
public abstract class AbstractType implements Type {

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
	public int compare(Object x, Object y) {
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
	public boolean isDirty(Object old, Object current, SharedSessionContractImplementor session)
			throws HibernateException {
		return !isSame( old, current );
	}

	@Override
	public boolean isAnyType() {
		return false;
	}

	@Override
	public boolean isModified(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException {
		return isDirty( old, current, session );
	}

	@Override
	public boolean isSame(Object x, Object y) throws HibernateException {
		return isEqual(x, y );
	}

	@Override
	public boolean isEqual(Object x, Object y) {
		return Objects.equals( x, y );
	}

	@Override
	public int getHashCode(Object x) {
		return x.hashCode();
	}

	@Override
	public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) {
		return isEqual( x, y );
	}

	@Override
	public int getHashCode(Object x, SessionFactoryImplementor factory) {
		return getHashCode( x );
	}

	@Override
	public Object replace(
			Object original,
			Object target,
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
			final AssociationType associationType = (AssociationType) this;
			return associationType.getForeignKeyDirection() == foreignKeyDirection;
		}
		else {
			return ForeignKeyDirection.FROM_PARENT == foreignKeyDirection;
		}
	}

	@Override
	public void beforeAssemble(Serializable cached, SharedSessionContractImplementor session) {}
}
