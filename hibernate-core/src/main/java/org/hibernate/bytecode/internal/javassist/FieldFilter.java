/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
	 * @param desc
	 * @param name
	 * @return true if the given field should be read intercepted; otherwise
	 * false.
	 */
	boolean handleRead(String desc, String name);

	/**
	 * Should the given field be write intercepted?
	 *
	 * @param desc
	 * @param name
	 * @return true if the given field should be write intercepted; otherwise
	 * false.
	 */
	boolean handleWrite(String desc, String name);

	boolean handleReadAccess(String fieldOwnerClassName, String fieldName);

	boolean handleWriteAccess(String fieldOwnerClassName, String fieldName);
}
