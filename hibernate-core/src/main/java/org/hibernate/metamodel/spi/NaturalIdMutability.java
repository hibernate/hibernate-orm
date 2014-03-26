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
package org.hibernate.metamodel.spi;

/**
 * A ternary boolean enum for describing the mutability aspects of an
 * attribute as a natural id.
 *
 * @author Steve Ebersole
 */
public enum NaturalIdMutability {
	/**
	 * The attribute is part of a mutable natural id
	 */
	MUTABLE,
	/**
	 * The attribute is part of a immutable natural id
	 */
	IMMUTABLE,
	/**
	 * The attribute is not part of any kind of natural id.
	 */
	NOT_NATURAL_ID
}
