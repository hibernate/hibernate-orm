/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.internal;

/**
 * @author Steve Ebersole
 */
public class Helper {
	/**
	 * Singleton access
	 */
	public static final Helper INSTANCE = new Helper();

	private Helper() {
	}

	public CdiLifecycleManagementStrategy getLifecycleManagementStrategy(boolean shouldRegistryManageLifecycle) {
		if ( shouldRegistryManageLifecycle ) {
			return JpaCdiLifecycleManagementStrategy.INSTANCE;
		}
		else {
			return StandardCdiLifecycleManagementStrategy.INSTANCE;
		}
	}
}
