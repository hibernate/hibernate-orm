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
package org.hibernate.boot.model.source.spi;

/**
 * @author Steve Ebersole
 */
public interface SizeSource {
	/**
	 * The specified length.  Will return {@code null} if none was specified.
	 *
	 * @return The length, or {@code null} if not defined.
	 */
	Integer getLength();

	/**
	 * The specified precision.  Will return {@code null} if none was specified.
	 *
	 * @return The precision, or {@code null} if not defined.
	 */
	Integer getPrecision();

	/**
	 * The specified scale.  Will return {@code null} if none was specified.
	 *
	 * @return The scale, or {@code null} if not defined.
	 */
	Integer getScale();
}
