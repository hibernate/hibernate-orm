/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy;
import java.io.Serializable;

/**
 * Convenience base class for SerializableProxy.
 * 
 * @author Gail Badner
 */
public abstract class AbstractSerializableProxy implements Serializable {
	private String entityName;
	private Serializable id;
	private Boolean readOnly;

	/**
	 * For serialization
	 */
	protected AbstractSerializableProxy() {
	}

	protected AbstractSerializableProxy(String entityName, Serializable id, Boolean readOnly) {
		this.entityName = entityName;
		this.id = id;
		this.readOnly = readOnly;
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
	 * This method should only be called during deserialization, beforeQuery associating the
	 * AbstractLazyInitializer with a session.
	 *
	 * @param li the read-only/modifiable setting to use when
	 * associated with a session; null indicates that the default should be used.
	 * @throws IllegalStateException if isReadOnlySettingAvailable() == true
	 */
	protected void setReadOnlyBeforeAttachedToSession(AbstractLazyInitializer li) {
		li.setReadOnlyBeforeAttachedToSession( readOnly );
	}
}
