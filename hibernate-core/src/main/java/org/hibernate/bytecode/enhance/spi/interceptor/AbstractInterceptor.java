/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi.interceptor;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractInterceptor implements SessionAssociableInterceptor {

	private final String entityName;
	private SessionAssociationMarkers sessionAssociation;

	public AbstractInterceptor(String entityName) {
		this.entityName = entityName;
	}

	public String getEntityName() {
		return entityName;
	}

	@Override
	public SharedSessionContractImplementor getLinkedSession() {
		return this.sessionAssociation != null ? this.sessionAssociation.session : null;
	}

	@Override
	public void setSession(SharedSessionContractImplementor session) {
		if ( session != null ) {
			this.sessionAssociation = session.getSessionAssociationMarkers();
		}
		else {
			unsetSession();
		}
	}

	@Override
	public void unsetSession() {
		if ( this.sessionAssociation != null ) {
			//We shouldn't mutate the original instance as it's shared across multiple entities,
			//but we can get a version of it which represents the same state except it doesn't have the session set:
			this.sessionAssociation = this.sessionAssociation.deAssociatedCopy();
		}
	}

	@Override
	public boolean allowLoadOutsideTransaction() {
		if ( this.sessionAssociation != null ) {
			return this.sessionAssociation.allowLoadOutsideTransaction;
		}
		else {
			return false;
		}
	}

	@Override
	public String getSessionFactoryUuid() {
		if ( this.sessionAssociation != null ) {
			return this.sessionAssociation.sessionFactoryUuid;
		}
		else {
			return null;
		}
	}

	/**
	 * Handle the case of reading an attribute.  The result is what is returned to the caller
	 */
	protected abstract Object handleRead(Object target, String attributeName, Object value);

	/**
	 * Handle the case of writing an attribute.  The result is what is set as the entity state
	 */
	protected abstract Object handleWrite(Object target, String attributeName, Object oldValue, Object newValue);

	@Override
	public boolean readBoolean(Object obj, String name, boolean oldValue) {
		return (Boolean) handleRead( obj, name, oldValue );
	}

	@Override
	public boolean writeBoolean(Object obj, String name, boolean oldValue, boolean newValue) {
		return (Boolean) handleWrite( obj, name, oldValue, newValue );
	}

	@Override
	public byte readByte(Object obj, String name, byte oldValue) {
		return (Byte) handleRead( obj, name, oldValue );
	}

	@Override
	public byte writeByte(Object obj, String name, byte oldValue, byte newValue) {
		return (Byte) handleWrite( obj, name, oldValue, newValue );
	}

	@Override
	public char readChar(Object obj, String name, char oldValue) {
		return (Character) handleRead( obj, name, oldValue );
	}

	@Override
	public char writeChar(Object obj, String name, char oldValue, char newValue) {
		return (char) handleWrite( obj, name, oldValue, newValue );
	}

	@Override
	public short readShort(Object obj, String name, short oldValue) {
		return (Short) handleRead( obj, name, oldValue );
	}

	@Override
	public short writeShort(Object obj, String name, short oldValue, short newValue) {
		return (Short) handleWrite( obj, name, oldValue, newValue );
	}

	@Override
	public int readInt(Object obj, String name, int oldValue) {
		return (Integer) handleRead( obj, name, oldValue );
	}

	@Override
	public int writeInt(Object obj, String name, int oldValue, int newValue) {
		return (Integer) handleWrite( obj, name, oldValue, newValue );
	}

	@Override
	public float readFloat(Object obj, String name, float oldValue) {
		return (Float) handleRead( obj, name, oldValue );
	}

	@Override
	public float writeFloat(Object obj, String name, float oldValue, float newValue) {
		return (Float) handleWrite( obj, name, oldValue, newValue );
	}

	@Override
	public double readDouble(Object obj, String name, double oldValue) {
		return (Double) handleRead( obj, name, oldValue );
	}

	@Override
	public double writeDouble(Object obj, String name, double oldValue, double newValue) {
		return (Double) handleWrite( obj, name, oldValue, newValue );
	}

	@Override
	public long readLong(Object obj, String name, long oldValue) {
		return (Long) handleRead( obj, name, oldValue );
	}

	@Override
	public long writeLong(Object obj, String name, long oldValue, long newValue) {
		return (Long) handleWrite( obj, name, oldValue, newValue );
	}

	@Override
	public Object readObject(Object obj, String name, Object oldValue) {
		return handleRead( obj, name, oldValue );
	}

	@Override
	public Object writeObject(Object obj, String name, Object oldValue, Object newValue) {
		return handleWrite( obj, name, oldValue, newValue );
	}
}
