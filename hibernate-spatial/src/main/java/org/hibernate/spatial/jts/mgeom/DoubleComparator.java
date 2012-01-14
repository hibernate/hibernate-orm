/*
 * $Id:$
 *
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for geographic data.
 *
 * Copyright © 2007-2010 Geovise BVBA
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, visit: http://www.hibernatespatial.org/
 */
package org.hibernate.spatial.jts.mgeom;

/**
 * This utility class is used to testsuite-suite doubles for equality
 *
 * @author Didier H. Besset <p/> Adapted from "Object-oriented implementation of
 *         numerical methods"
 */
//TODO: This class should be removed.
public final class DoubleComparator {

	private final static int radix = computeRadix();

	private final static double machinePrecision = computeMachinePrecision();

	private final static double defaultNumericalPrecision = Math
			.sqrt( machinePrecision );

	private static int computeRadix() {
		int radix = 0;
		double a = 1.0d;
		double tmp1, tmp2;
		do {
			a += a;
			tmp1 = a + 1.0d;
			tmp2 = tmp1 - a;
		} while ( tmp2 - 1.0d != 0.0d );
		double b = 1.0d;
		while ( radix == 0 ) {
			b += b;
			tmp1 = a + b;
			radix = (int) ( tmp1 - a );
		}
		return radix;
	}

	public static int getRadix() {
		return radix;
	}

	private static double computeMachinePrecision() {
		double floatingRadix = getRadix();
		double inverseRadix = 1.0d / floatingRadix;
		double machinePrecision = 1.0d;
		double tmp = 1.0d + machinePrecision;
		while ( tmp - 1.0d != 0.0 ) {
			machinePrecision *= inverseRadix;
			tmp = 1.0d + machinePrecision;
		}
		return machinePrecision;
	}

	public static double getMachinePrecision() {
		return machinePrecision;
	}

	public static double defaultNumericalPrecision() {
		return defaultNumericalPrecision;
	}

	public static boolean equals(double a, double b) {
		return equals( a, b, defaultNumericalPrecision() );
	}

	public static boolean equals(double a, double b, double precision) {
		double norm = Math.max( Math.abs( a ), Math.abs( b ) );
		boolean result = norm < precision || Math.abs( a - b ) < precision * norm;
		return result || ( Double.isNaN( a ) && Double.isNaN( b ) );
	}

	public static void main(String[] args) {
		System.out.println( "Machine precision = " + getMachinePrecision() );
		System.out.println( "Radix = " + getRadix() );
		System.out.println(
				"default numerical precision = "
						+ defaultNumericalPrecision()
		);
	}
}
