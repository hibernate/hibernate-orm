/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

/**
 * Implementation of DelayedDropRegistry for cases when the delayed-drop portion of
 * "create-drop" is not valid.
 *
 * @author Steve Ebersole
 */
public class DelayedDropRegistryNotAvailableImpl implements DelayedDropRegistry {
	/**
	 * Singleton access
	 */
	public static final DelayedDropRegistryNotAvailableImpl INSTANCE = new DelayedDropRegistryNotAvailableImpl();

	@Override
	public void registerOnCloseAction(DelayedDropAction action) {
		throw new SchemaManagementException(
				"DelayedDropRegistry is not available in this context.  'create-drop' action is not valid"
		);
	}
}
