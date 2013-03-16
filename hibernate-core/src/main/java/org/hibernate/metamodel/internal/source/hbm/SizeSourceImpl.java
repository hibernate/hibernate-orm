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
package org.hibernate.metamodel.internal.source.hbm;

import org.hibernate.metamodel.spi.source.SizeSource;

/**
 * @author Gail Badner
 */
public class SizeSourceImpl implements SizeSource {

	private final Integer length;
	private final Integer precision;
	private final Integer scale;

	public SizeSourceImpl(Integer precision, Integer scale, Integer length) {
		this.precision = precision;
		this.scale = scale;
		this.length = length;
	}

	public boolean isLengthDefined() {
		return length != null;
	}

	public int getLength() {
		if ( length == null ) {
			throw new UnsupportedOperationException( "length is undefined." );
		}
		return length;
	}

	public boolean isPrecisionDefined() {
		return precision != null;
	}

	public int getPrecision() {
		if ( precision == null ) {
			throw new UnsupportedOperationException( "precision is undefined." );
		}
		return precision;
	}

	public boolean isScaleDefined() {
		return scale != null;
	}

	public int getScale() {
		if ( scale == null ) {
			throw new UnsupportedOperationException( "scale is undefined." );
		}
		return scale;
	}
}
