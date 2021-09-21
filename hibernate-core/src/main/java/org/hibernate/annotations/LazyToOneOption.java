/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

/**
 * Lazy options available for a ToOne association.
 *
 * @author Emmanuel Bernard
 */
public enum LazyToOneOption {
	/**
	 * Eagerly load the association.
	 */
	FALSE,
	/**
	 * Lazy, give back a proxy which will be loaded when the state is requested.
	 *
	 * This should be the preferred option.
	 */
	PROXY,
	/**
	 * Lazy, give back the real object loaded when a reference is requested.
	 *
	 * Bytecode enhancement is mandatory for this option.  Falls back to {@link #PROXY}
	 * if the class is not enhanced.  This option should be avoided unless you can't afford
	 * the use of proxies
	 */
	NO_PROXY
}
