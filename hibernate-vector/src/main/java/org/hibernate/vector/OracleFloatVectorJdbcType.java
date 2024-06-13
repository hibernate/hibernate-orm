/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.vector;

import java.util.Arrays;
import java.util.BitSet;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleTypes;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Specialized type mapping for single-precision floating-point vector {@link SqlTypes#VECTOR_FLOAT32} SQL data type for Oracle.
 *
 * @author Hassan AL Meftah
 */

public class OracleFloatVectorJdbcType extends AbstractOracleVectorJdbcType {


	private static final float[] EMPTY = new float[0];

	public OracleFloatVectorJdbcType(JdbcType elementJdbcType, boolean isVectorSupported) {
		super( elementJdbcType, isVectorSupported );
	}

	@Override
	public void appendWriteExpression(String writeExpression, SqlAppender appender, Dialect dialect) {
		appender.append( "to_vector(" );
		appender.append( writeExpression );
		appender.append( ", *, FLOAT32)" );
	}

	@Override
	public String getFriendlyName() {
		return "VECTOR_FLOAT32";
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.VECTOR_FLOAT32;
	}

	@Override
	protected float[] getVectorArray(String string) {
		if ( string == null ) {
			return null;
		}
		if ( string.length() == 2 ) {
			return EMPTY;
		}
		final BitSet commaPositions = new BitSet();
		int size = 1;
		for ( int i = 1; i < string.length(); i++ ) {
			final char c = string.charAt( i );
			if ( c == ',' ) {
				commaPositions.set( i );
				size++;
			}
		}
		final float[] result = new float[size];
		int doubleStartIndex = 1;
		int commaIndex;
		int index = 0;
		while ( ( commaIndex = commaPositions.nextSetBit( doubleStartIndex ) ) != -1 ) {
			result[index++] = Float.parseFloat( string.substring( doubleStartIndex, commaIndex ) );
			doubleStartIndex = commaIndex + 1;
		}
		result[index] = Float.parseFloat( string.substring( doubleStartIndex, string.length() - 1 ) );
		return result;
	}

	@Override
	protected <T> String getStringVector(T vector, JavaType<T> javaTypeDescriptor, WrapperOptions options) {
		return Arrays.toString( javaTypeDescriptor.unwrap( vector, float[].class, options ) );
	}

	protected Class<?> getNativeJavaType() {
		return float[].class;
	}

	protected int getNativeTypeCode() {
		return OracleTypes.VECTOR_FLOAT32;
	}
}
