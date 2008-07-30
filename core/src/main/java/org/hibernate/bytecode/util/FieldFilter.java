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
 *
 */
package org.hibernate.bytecode.util;

/**
 * Used to determine whether a field reference should be instrumented.
 *
 * @author Steve Ebersole
 */
public interface FieldFilter {
	/**
	 * Should this field definition be instrumented?
	 *
	 * @param className The name of the class currently being processed
	 * @param fieldName The name of the field being checked.
	 * @return True if we should instrument this field.
	 */
	public boolean shouldInstrumentField(String className, String fieldName);

	/**
	 * Should we instrument *access to* the given field.  This differs from
	 * {@link #shouldInstrumentField} in that here we are talking about a particular usage of
	 * a field.
	 *
	 * @param transformingClassName The class currently being transformed.
	 * @param fieldOwnerClassName The name of the class owning this field being checked.
	 * @param fieldName The name of the field being checked.
	 * @return True if this access should be transformed.
	 */
	public boolean shouldTransformFieldAccess(String transformingClassName, String fieldOwnerClassName, String fieldName);
}
