/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.spi;

/**
 * @author Gail Badner
 */
public interface SizeSource {
	/**
	 * Is length defined?
	 *
	 * @return true, if length is defined; false, otherwise.
	 */
	boolean isLengthDefined();

	/**
	 * If length is defined (as determined by {@link #isLengthDefined()}), then
	 * the length is returned.
	 *
	 * @return the length, if defined.
	 * @throws UnsupportedOperationException if length is not defined.
	 */
	int getLength();

	/**
	 * Is precision defined?
	 *
	 * @return true, if precision is defined; false, otherwise.
	 */
	boolean isPrecisionDefined();

	/**
	 * If precision is defined (as determined by {@link #isPrecisionDefined()}), then
	 * the precision is returned.
	 *
	 * @return the precision, if defined.
	 * @throws UnsupportedOperationException if precision is not defined.
	 *
	 * @see {@link #isPrecisionDefined()}
	 */
	int getPrecision();

	/**
	 * Is scale defined?
	 *
	 * @return true, if scale is defined; false, otherwise.
	 */
	boolean isScaleDefined();


	/**
	 * If scale is defined (as determined by {@link #isScaleDefined()}), then
	 * the scale is returned.
	 *
	 * @return the scale, if defined.
	 * @throws UnsupportedOperationException if scale is not defined.
	 *
	 * @see {@link #isScaleDefined()}
	 */
	int getScale();

	SizeSource NULL = new SizeSource() {
		@Override
		public boolean isLengthDefined() {
			return false;
		}

		@Override
		public int getLength() {
			return 0;
		}

		@Override
		public boolean isPrecisionDefined() {
			return false;
		}

		@Override
		public int getPrecision() {
			return 0;
		}

		@Override
		public boolean isScaleDefined() {
			return false;
		}

		@Override
		public int getScale() {
			return 0;
		}
	};


}
