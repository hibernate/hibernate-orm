/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.bytecode.enhance.spi.interceptor;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractInterceptor implements SessionAssociableInterceptor {
	private final String entityName;

	private transient SharedSessionContractImplementor session;
	private boolean allowLoadOutsideTransaction;
	private String sessionFactoryUuid;

	@SuppressWarnings("WeakerAccess")
	public AbstractInterceptor(String entityName) {
		this.entityName = entityName;
	}

	public String getEntityName() {
		return entityName;
	}

	@Override
	public SharedSessionContractImplementor getLinkedSession() {
		return session;
	}

	@Override
	public void setSession(SharedSessionContractImplementor session) {
		this.session = session;
		if ( session != null && !allowLoadOutsideTransaction ) {
			this.allowLoadOutsideTransaction = session.getFactory().getSessionFactoryOptions().isInitializeLazyStateOutsideTransactionsEnabled();
			if ( this.allowLoadOutsideTransaction ) {
				this.sessionFactoryUuid = session.getFactory().getUuid();
			}
		}
	}

	@Override
	public void unsetSession() {
		this.session = null;
	}

	@Override
	public boolean allowLoadOutsideTransaction() {
		return allowLoadOutsideTransaction;
	}

	@Override
	public String getSessionFactoryUuid() {
		return sessionFactoryUuid;
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
