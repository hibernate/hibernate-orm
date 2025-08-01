/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector;

public class VectorTestHelper {

	public static double cosineDistance(float[] f1, float[] f2) {
		return 1D - innerProduct( f1, f2 ) / ( euclideanNorm( f1 ) * euclideanNorm( f2 ) );
	}

	public static double cosineDistance(double[] f1, double[] f2) {
		return 1D - innerProduct( f1, f2 ) / ( euclideanNorm( f1 ) * euclideanNorm( f2 ) );
	}

	public static double cosineDistance(byte[] f1, byte[] f2) {
		return 1D - innerProduct( f1, f2 ) / ( euclideanNorm( f1 ) * euclideanNorm( f2 ) );
	}

	public static double euclideanDistance(float[] f1, float[] f2) {
		assert f1.length == f2.length;
		double result = 0;
		for ( int i = 0; i < f1.length; i++ ) {
			result += Math.pow( (double) f1[i] - f2[i], 2 );
		}
		return Math.sqrt( result );
	}

	public static double euclideanDistance(double[] f1, double[] f2) {
		assert f1.length == f2.length;
		double result = 0;
		for ( int i = 0; i < f1.length; i++ ) {
			result += Math.pow( (double) f1[i] - f2[i], 2 );
		}
		return Math.sqrt( result );
	}

	public static double euclideanDistance(byte[] f1, byte[] f2) {
		assert f1.length == f2.length;
		double result = 0;
		for ( int i = 0; i < f1.length; i++ ) {
			result += Math.pow( (double) f1[i] - f2[i], 2 );
		}
		return Math.sqrt( result );
	}

	public static double taxicabDistance(float[] f1, float[] f2) {
		return norm( f1 ) - norm( f2 );
	}

	public static double taxicabDistance(double[] f1, double[] f2) {
		return norm( f1 ) - norm( f2 );
	}

	public static double taxicabDistance(byte[] f1, byte[] f2) {
		return norm( f1 ) - norm( f2 );
	}

	public static double innerProduct(float[] f1, float[] f2) {
		assert f1.length == f2.length;
		double result = 0;
		for ( int i = 0; i < f1.length; i++ ) {
			result += ( (double) f1[i] ) * ( (double) f2[i] );
		}
		return result;
	}

	public static double innerProduct(double[] f1, double[] f2) {
		assert f1.length == f2.length;
		double result = 0;
		for ( int i = 0; i < f1.length; i++ ) {
			result += ( (double) f1[i] ) * ( (double) f2[i] );
		}
		return result;
	}

	public static double innerProduct(byte[] f1, byte[] f2) {
		assert f1.length == f2.length;
		double result = 0;
		for ( int i = 0; i < f1.length; i++ ) {
			result += ( (double) f1[i] ) * ( (double) f2[i] );
		}
		return result;
	}

	public static double hammingDistance(float[] f1, float[] f2) {
		assert f1.length == f2.length;
		int distance = 0;
		for ( int i = 0; i < f1.length; i++ ) {
			if ( !( f1[i] == f2[i] ) ) {
				distance++;
			}
		}
		return distance;
	}

	public static double hammingDistance(double[] f1, double[] f2) {
		assert f1.length == f2.length;
		int distance = 0;
		for (int i = 0; i < f1.length; i++) {
			if (!(f1[i] == f2[i])) {
				distance++;
			}
		}
		return distance;
	}

	public static double hammingDistance(byte[] f1, byte[] f2) {
		assert f1.length == f2.length;
		int distance = 0;
		for (int i = 0; i < f1.length; i++) {
			if (!(f1[i] == f2[i])) {
				distance++;
			}
		}
		return distance;
	}

	public static double euclideanNorm(float[] f) {
		double result = 0;
		for ( double v : f ) {
			result += Math.pow( v, 2 );
		}
		return Math.sqrt( result );
	}

	public static double euclideanNorm(double[] f) {
		double result = 0;
		for ( double v : f ) {
			result += Math.pow( v, 2 );
		}
		return Math.sqrt( result );
	}

	public static double euclideanNorm(byte[] f) {
		double result = 0;
		for ( double v : f ) {
			result += Math.pow( v, 2 );
		}
		return Math.sqrt( result );
	}

	public static double norm(float[] f) {
		double result = 0;
		for ( double v : f ) {
			result += Math.abs( v );
		}
		return result;
	}

	public static double norm(double[] f) {
		double result = 0;
		for ( double v : f ) {
			result += Math.abs( v );
		}
		return result;
	}

	public static double norm(byte[] f) {
		double result = 0;
		for ( double v : f ) {
			result += Math.abs( v );
		}
		return result;
	}
}
