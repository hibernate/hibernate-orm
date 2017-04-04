/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations.reflection;

import org.hibernate.annotations.common.reflection.Filter;

/**
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class PersistentAttributeFilter implements Filter {
	/**
	 * Singleton access
	 */
	public static final PersistentAttributeFilter INSTANCE = new PersistentAttributeFilter();

	@Override
	public boolean returnStatic() {
		return false;
	}

	@Override
	public boolean returnTransient() {
		return false;
	}
}
