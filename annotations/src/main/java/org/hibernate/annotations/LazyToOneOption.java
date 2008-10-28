/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
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
 */
package org.hibernate.annotations;

/**
 * Lazy options available for a ToOne association
 *
 * @author Emmanuel Bernard
 */
public enum LazyToOneOption {
	/** eagerly load the association */
	FALSE,
	/**
	 * Lazy, give back a proxy which will be loaded when the state is requested
	 * This should be the prefered option
	 */
	PROXY,
	/** Lazy, give back the real object loaded when a reference is requested
	 * (Bytecode enhancement is mandatory for this option, fall back to PROXY
	 * if the class is not enhanced)
	 * This option should be avoided unless you can't afford the use of proxies
	 */
	NO_PROXY
}
