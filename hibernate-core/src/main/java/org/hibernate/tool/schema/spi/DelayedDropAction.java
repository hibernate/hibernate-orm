/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.service.ServiceRegistry;

/**
 * Memento representing the dropping of a schema as part of create-drop
 * hbm2ddl.auto handling.  This memento is registered with the
 * SessionFactory and executed as the SessionFactory is closing.
 * <p/>
 * Implementations should be Serializable
 *
 * @author Steve Ebersole
 */
public interface DelayedDropAction {
	/**
	 * Perform the delayed schema drop.
	 *
	 * @param serviceRegistry Access to the ServiceRegistry
	 */
	void perform(ServiceRegistry serviceRegistry);
}
