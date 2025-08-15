/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.spi.TypeConfiguration;

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

	public static double cosineDistanceBinary(byte[] f1, byte[] f2) {
		return 1D - innerProductBinary( f1, f2 ) / ( euclideanNormBinary( f1 ) * euclideanNormBinary( f2 ) );
	}

	public static double euclideanDistance(float[] f1, float[] f2) {
		return Math.sqrt( euclideanSquaredDistance( f1, f2 ) );
	}

	public static double euclideanDistance(double[] f1, double[] f2) {
		return Math.sqrt( euclideanSquaredDistance( f1, f2 ) );
	}

	public static double euclideanDistance(byte[] f1, byte[] f2) {
		return Math.sqrt( euclideanSquaredDistance( f1, f2 ) );
	}

	public static double euclideanDistanceBinary(byte[] f1, byte[] f2) {
		// On bit level, the two distance functions are equivalent
		return Math.sqrt( hammingDistanceBinary( f1, f2 ) );
	}

	public static double euclideanSquaredDistance(float[] f1, float[] f2) {
		assert f1.length == f2.length;
		double result = 0;
		for ( int i = 0; i < f1.length; i++ ) {
			result += Math.pow( (double) f1[i] - f2[i], 2 );
		}
		return result;
	}

	public static double euclideanSquaredDistance(double[] f1, double[] f2) {
		assert f1.length == f2.length;
		double result = 0;
		for ( int i = 0; i < f1.length; i++ ) {
			result += Math.pow( (double) f1[i] - f2[i], 2 );
		}
		return result;
	}

	public static double euclideanSquaredDistance(byte[] f1, byte[] f2) {
		assert f1.length == f2.length;
		double result = 0;
		for ( int i = 0; i < f1.length; i++ ) {
			result += Math.pow( (double) f1[i] - f2[i], 2 );
		}
		return result;
	}

	public static double euclideanSquaredDistanceBinary(byte[] f1, byte[] f2) {
		// On bit level, the two distance functions are equivalent
		return hammingDistanceBinary( f1, f2 );
	}

	public static double taxicabDistance(float[] f1, float[] f2) {
		assert f1.length == f2.length;
		double result = 0;
		for ( int i = 0; i < f1.length; i++ ) {
			result += Math.abs( f1[i] - f2[i] );
		}
		return result;
	}

	public static double taxicabDistance(double[] f1, double[] f2) {
		assert f1.length == f2.length;
		double result = 0;
		for ( int i = 0; i < f1.length; i++ ) {
			result += Math.abs( f1[i] - f2[i] );
		}
		return result;
	}

	public static double taxicabDistance(byte[] f1, byte[] f2) {
		assert f1.length == f2.length;
		double result = 0;
		for ( int i = 0; i < f1.length; i++ ) {
			result += Math.abs( f1[i] - f2[i] );
		}
		return result;
	}

	public static double taxicabDistanceBinary(byte[] f1, byte[] f2) {
		// On bit level, the two distance functions are equivalent
		return hammingDistanceBinary( f1, f2 );
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

	public static double innerProductBinary(byte[] f1, byte[] f2) {
		assert f1.length == f2.length;
		double result = 0;
		for ( int i = 0; i < f1.length; i++ ) {
			result += Integer.bitCount( f1[i] & f2[i] );
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

	public static double hammingDistanceBinary(byte[] f1, byte[] f2) {
		assert f1.length == f2.length;
		int distance = 0;
		for (int i = 0; i < f1.length; i++) {
			distance += Integer.bitCount( f1[i] ^ f2[i] );
		}
		return distance;
	}

	public static double euclideanNorm(float[] f) {
		double result = 0;
		for ( float v : f ) {
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
		for ( byte v : f ) {
			result += Math.pow( v, 2 );
		}
		return Math.sqrt( result );
	}

	public static float[] euclideanNormalize(float[] f) {
		final double norm = euclideanNorm( f );
		final float[] result = new float[f.length];
		for ( int i = 0; i < f.length; i++ ) {
			result[i] = (float) (f[i] / norm);
		}
		return result;
	}

	public static float[] euclideanNormalize(double[] f) {
		final double norm = euclideanNorm( f );
		final float[] result = new float[f.length];
		for ( int i = 0; i < f.length; i++ ) {
			result[i] = (float) (f[i] / norm);
		}
		return result;
	}

	public static float[] euclideanNormalize(byte[] f) {
		final double norm = euclideanNorm( f );
		final float[] result = new float[f.length];
		for ( int i = 0; i < f.length; i++ ) {
			result[i] = (float) (f[i] / norm);
		}
		return result;
	}

	public static double euclideanNormBinary(byte[] f) {
		double result = 0;
		for ( byte v : f ) {
			result += Integer.bitCount( v );
		}
		return Math.sqrt( result );
	}

	public static double jaccardDistanceBinary(byte[] f1, byte[] f2) {
		assert f1.length == f2.length;
		int intersectionSum = 0;
		int unionSum = 0;
		for (int i = 0; i < f1.length; i++) {
			intersectionSum += Integer.bitCount( f1[i] & f2[i] );
			unionSum += Integer.bitCount( f1[i] | f2[i] );
		}
		return 1d - (double) intersectionSum / unionSum;
	}

	public static String normalizeVectorString(String vector) {
		return vector.replace( "E+000", "" )
				.replace( ".0", "" );
	}

	public static String vectorSparseStringLiteral(float[] vector, SessionImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();
		final JdbcLiteralFormatter<SparseFloatVector> literalFormatter = typeConfiguration.getJdbcTypeRegistry()
				.getDescriptor( SqlTypes.SPARSE_VECTOR_FLOAT32 )
				.getJdbcLiteralFormatter( typeConfiguration.getJavaTypeRegistry().getDescriptor( SparseFloatVector.class ) );
		final String jdbcLiteral = literalFormatter.toJdbcLiteral(
				new SparseFloatVector( vector ),
				sessionFactory.getJdbcServices().getDialect(),
				session
		);
		final int start = jdbcLiteral.indexOf( '\'' );
		final int end = jdbcLiteral.indexOf( '\'', start + 1 );
		return jdbcLiteral.substring( start + 1, end ).replace( ".0", "" );
	}

	public static String vectorSparseStringLiteral(double[] vector, SessionImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();
		final JdbcLiteralFormatter<SparseDoubleVector> literalFormatter = typeConfiguration.getJdbcTypeRegistry()
				.getDescriptor( SqlTypes.SPARSE_VECTOR_FLOAT64 )
				.getJdbcLiteralFormatter( typeConfiguration.getJavaTypeRegistry().getDescriptor( SparseDoubleVector.class ) );
		final String jdbcLiteral = literalFormatter.toJdbcLiteral(
				new SparseDoubleVector( vector ),
				sessionFactory.getJdbcServices().getDialect(),
				session
		);
		final int start = jdbcLiteral.indexOf( '\'' );
		final int end = jdbcLiteral.indexOf( '\'', start + 1 );
		return jdbcLiteral.substring( start + 1, end ).replace( ".0", "" );
	}

	public static String vectorSparseStringLiteral(byte[] vector, SessionImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();
		final JdbcLiteralFormatter<SparseByteVector> literalFormatter = typeConfiguration.getJdbcTypeRegistry()
				.getDescriptor( SqlTypes.SPARSE_VECTOR_INT8 )
				.getJdbcLiteralFormatter( typeConfiguration.getJavaTypeRegistry().getDescriptor( SparseByteVector.class ) );
		final String jdbcLiteral = literalFormatter.toJdbcLiteral(
				new SparseByteVector( vector ),
				sessionFactory.getJdbcServices().getDialect(),
				session
		);
		final int start = jdbcLiteral.indexOf( '\'' );
		final int end = jdbcLiteral.indexOf( '\'', start + 1 );
		return jdbcLiteral.substring( start + 1, end );
	}

	public static String vectorBinaryStringLiteral(byte[] vector, SessionImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final JdbcLiteralFormatter<byte[]> literalFormatter = sessionFactory.getTypeConfiguration()
				.getBasicTypeRegistry().resolve( StandardBasicTypes.VECTOR_BINARY ).getJdbcLiteralFormatter();
		final String jdbcLiteral = literalFormatter.toJdbcLiteral(
				vector,
				sessionFactory.getJdbcServices().getDialect(),
				session
		);
		final int start = jdbcLiteral.indexOf( '\'' );
		final int end = jdbcLiteral.indexOf( '\'', start + 1 );
		return jdbcLiteral.substring( start + 1, end );
	}
}
