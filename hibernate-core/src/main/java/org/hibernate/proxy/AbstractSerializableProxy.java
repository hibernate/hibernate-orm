/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
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
	 * This method should only be called during deserialization, before associating the
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
