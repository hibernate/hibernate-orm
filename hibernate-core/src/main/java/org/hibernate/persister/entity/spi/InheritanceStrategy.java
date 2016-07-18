/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity.spi;

/**
 * @author Steve Ebersole
 */
public enum InheritanceStrategy {
	/**
	 * No inheritance.  Similar to {@link #DISCRIMINATOR} (single table) but with
	 * no discriminator column/formula.
	 */
	NONE,
	/**
	 * An inher
	 */
	DISCRIMINATOR,
	UNION,
	JOINED
}
