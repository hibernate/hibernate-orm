/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy;

import java.io.Serializable;

/**
 * Convenience base class for the serialized form of {@link AbstractLazyInitializer}.
 * 
 * @author Gail Badner
 */
public abstract class AbstractSerializableProxy implements Serializable {
	private String entityName;
	private Serializable id;
	private Boolean readOnly;
	private String sessionFactoryUuid;
	private boolean allowLoadOutsideTransaction;

	/**
	 * @deprecated This constructor was initially intended for serialization only, and is not useful anymore.
	 * In any case it should not be relied on by user code.
	 */
	@Deprecated
	protected AbstractSerializableProxy() {
	}

	/**
	 * @deprecated use {@link #AbstractSerializableProxy(String, Serializable, Boolean, String, boolean)} instead.
	 */
	@Deprecated
	protected AbstractSerializableProxy(String entityName, Serializable id, Boolean readOnly) {
		this( entityName, id, readOnly, null, false );
	}

	protected AbstractSerializableProxy(String entityName, Serializable id, Boolean readOnly,
			String sessionFactoryUuid, boolean allowLoadOutsideTransaction) {
		this.entityName = entityName;
		this.id = id;
		this.readOnly = readOnly;
		this.sessionFactoryUuid = sessionFactoryUuid;
		this.allowLoadOutsideTransaction = allowLoadOutsideTransaction;
	}

	protected String getEntityName() {
		return entityName;
	}

	protected Serializable getId() {
		return id;
	}

	/**
	 * Set the read-only/modifiable setting from this object in an AbstractLazyInitializer.
	 *
	 * This method should only be called during deserialization, before associating the
	 * AbstractLazyInitializer with a session.
	 *
	 * @param li the read-only/modifiable setting to use when
	 * associated with a session; null indicates that the default should be used.
	 * @throws IllegalStateException if isReadOnlySettingAvailable() == true
	 *
	 * @deprecated Use {@link #afterDeserialization(AbstractLazyInitializer)} instead.
	 */
	@Deprecated
	protected void setReadOnlyBeforeAttachedToSession(AbstractLazyInitializer li) {
		li.afterDeserialization( readOnly, null, false );
	}

	/**
	 * Initialize an {@link AbstractLazyInitializer} after deserialization.
	 *
	 * This method should only be called during deserialization,
	 * before associating the AbstractLazyInitializer with a session.
	 *
	 * @param li the {@link AbstractLazyInitializer} to initialize.
	 */
	protected void afterDeserialization(AbstractLazyInitializer li) {
		li.afterDeserialization( readOnly, sessionFactoryUuid, allowLoadOutsideTransaction );
	}
}
