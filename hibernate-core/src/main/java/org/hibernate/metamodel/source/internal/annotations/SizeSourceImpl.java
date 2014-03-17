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
package org.hibernate.metamodel.source.internal.annotations;

import org.hibernate.metamodel.source.spi.SizeSource;

/**
 * @author Gail Badner
 */
public class SizeSourceImpl implements SizeSource {

	private final int length;
	private final int precision;
	private final int scale;

	public SizeSourceImpl(int precision, int scale, int length) {
		this.precision = precision;
		this.scale = scale;
		this.length = length;
	}

	public boolean isLengthDefined() {
		return true;
	}

	public int getLength() {
		return length;
	}

	public boolean isPrecisionDefined() {
		return true;
	}

	public int getPrecision() {
		return precision;
	}

	public boolean isScaleDefined() {
		return true;
	}

	public int getScale() {
		return scale;
	}
}
