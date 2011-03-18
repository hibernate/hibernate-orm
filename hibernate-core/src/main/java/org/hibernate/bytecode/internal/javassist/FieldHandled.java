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
 * Interface introduced to the enhanced class in order to be able to
 * inject a {@link FieldHandler} to define the interception behavior.
 *
 * @author Muga Nishizawa
 */
public interface FieldHandled {
	/**
	 * Inject the field interception handler to be used.
	 *
	 * @param handler The field interception handler.
	 */
	public void setFieldHandler(FieldHandler handler);

	/**
	 * Access to the current field interception handler.
	 *
	 * @return The current field interception handler.
	 */
	public FieldHandler getFieldHandler();
}
