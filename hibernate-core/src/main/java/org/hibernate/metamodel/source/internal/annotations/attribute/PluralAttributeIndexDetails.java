/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.annotations.attribute;

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.AttributeTypeResolver;
import org.hibernate.metamodel.spi.PluralAttributeIndexNature;

/**
 * Presents metadata about the elements of a plural attribute.
 * <p/>
 * Note that this is actually only valid for persistent lists and maps.  For
 * all other plural attribute natures,
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeIndexDetails {
	/**
	 * Get the descriptor for the Java type of the collection index.
	 *
	 * @return The descriptor for the Java type of the collection index.
	 */
	public JavaTypeDescriptor getJavaType();

	/**
	 * Get the nature of the collection index.  To a large degree, this is
	 * used to know what further, more specific type-casts can be performed.
	 *
	 * @return The nature of the collection index
	 */
	public PluralAttributeIndexNature getIndexNature();

	/**
	 * Get the type resolver for the collection index.
	 *
	 * @return The type resolver for the collection index.
	 */
	public AttributeTypeResolver getTypeResolver();
}
