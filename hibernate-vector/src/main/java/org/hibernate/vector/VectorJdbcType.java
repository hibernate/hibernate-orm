/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.vector;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.BitSet;

import org.hibernate.dialect.Dialect;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

public class VectorJdbcType extends ArrayJdbcType {

	private static final float[] EMPTY = new float[0];
	public VectorJdbcType(JdbcType elementJdbcType) {
		super( elementJdbcType );
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.VECTOR;
	}

	@Override
	public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().resolveDescriptor( float[].class );
	}

	@Override
	public void appendWriteExpression(String writeExpression, SqlAppender appender, Dialect dialect) {
		appender.append( "cast(" );
		appender.append( writeExpression );
		appender.append( " as vector)" );
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaTypeDescriptor) {
		return new BasicExtractor<>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( getFloatArray( rs.getString( paramIndex ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( getFloatArray( statement.getString( index ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( getFloatArray( statement.getString( name ) ), options );
			}

			private float[] getFloatArray(String string) {
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
				int floatStartIndex = 1;
				int commaIndex;
				int index = 0;
				while ( ( commaIndex = commaPositions.nextSetBit( floatStartIndex ) ) != -1 ) {
					result[index++] = Float.parseFloat( string.substring( floatStartIndex, commaIndex ) );
					floatStartIndex = commaIndex + 1;
				}
				result[index] = Float.parseFloat( string.substring( floatStartIndex, string.length() - 1 ) );
				return result;
			}
		};
	}
}
