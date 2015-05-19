/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.javassist;

/**
 * Contract for deciding whether fields should be read and/or write intercepted.
 *
 * @author Muga Nishizawa
 * @author Steve Ebersole
 */
public interface FieldFilter {
	/**
	 * Should the given field be read intercepted?
	 *
	 * @param desc The field descriptor
	 * @param name The field name
	 *
	 * @return true if the given field should be read intercepted; otherwise
	 * false.
	 */
	boolean handleRead(String desc, String name);

	/**
	 * Should the given field be write intercepted?
	 *
	 * @param desc The field descriptor
	 * @param name The field name
	 *
	 * @return true if the given field should be write intercepted; otherwise
	 * false.
	 */
	boolean handleWrite(String desc, String name);

	/**
	 * Should read access to the given field be intercepted?
	 *
	 * @param fieldOwnerClassName The class where the field being accessed is defined
	 * @param fieldName The name of the field being accessed
	 *
	 * @return true if the given field read access should be write intercepted; otherwise
	 * false.
	 */
	boolean handleReadAccess(String fieldOwnerClassName, String fieldName);

	/**
	 * Should write access to the given field be intercepted?
	 *
	 * @param fieldOwnerClassName The class where the field being accessed is defined
	 * @param fieldName The name of the field being accessed
	 *
	 * @return true if the given field write access should be write intercepted; otherwise
	 * false.
	 */
	boolean handleWriteAccess(String fieldOwnerClassName, String fieldName);
}
