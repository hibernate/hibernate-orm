/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.exec.results.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.EntityMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.sqm.exec.results.spi.ResultSetProcessingOptions;
import org.hibernate.sql.sqm.exec.results.spi.ReturnReader;
import org.hibernate.sql.sqm.exec.results.spi.RowProcessingState;
import org.hibernate.type.CompositeType;
import org.hibernate.type.spi.Type;
import org.hibernate.type.spi.descriptor.TypeDescriptorRegistryAccess;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class ReturnReaderScalarImpl implements ReturnReader {
	private final int startPosition;
	private final Type returnType;

	public ReturnReaderScalarImpl(int startPosition, Type returnType) {
		this.startPosition = startPosition;
		this.returnType = returnType;

		assert returnType != null;
	}

	@Override
	public Class getReturnedJavaType() {
		return returnType.getJavaTypeDescriptor().getJavaTypeClass();
	}

	@Override
	public int getNumberOfColumnsRead(SessionFactoryImplementor sessionFactory) {
		return returnType.getColumnSpan();
	}

	@Override
	public void readBasicValues(RowProcessingState processingState, ResultSetProcessingOptions options) {
		// nothing to do
	}

	@Override
	public void resolveBasicValues(RowProcessingState processingState, ResultSetProcessingOptions options) {
		// nothing to do
	}

	@Override
	public Object assemble(RowProcessingState processingState, ResultSetProcessingOptions options) throws SQLException {
		// for now we assume basic types with no attribute conversion etc.
		// a more correct implementation requires the "positional read" changes to Type.

		final SharedSessionContractImplementor session = processingState.getResultSetProcessingState().getSession();
		final ResultSet resultSet = processingState.getResultSetProcessingState().getResultSet();

		final int columnSpan = returnType.getColumnSpan();
		final int[] jdbcTypes = returnType.sqlTypes( session.getFactory() );
		if ( columnSpan > 1 ) {
			// has to be a CompositeType for now (and a very basic, one-level one)...
			final CompositeType ctype = (CompositeType) returnType;
			final Object[] values = new Object[ columnSpan ];
			for ( int i = 0; i < columnSpan; i++ ) {
				values[i] = readResultValue( resultSet, startPosition+i, jdbcTypes[i], session );
			}
			try {
				final Object result = ctype.getReturnedClass().newInstance();
				ctype.setPropertyValues( result, values, EntityMode.POJO );
				return result;
			}
			catch (Exception e) {
				throw new RuntimeException( "Unable to instantiate composite : " +  ctype.getReturnedClass().getName(), e );
			}
		}
		else {
			return readResultValue( resultSet, startPosition, jdbcTypes[0], session );
		}
	}

	private Object readResultValue(ResultSet resultSet, int position, int jdbcType, SharedSessionContractImplementor session) throws SQLException {
		final TypeDescriptorRegistryAccess typeDescriptorRegistryAccess = session.getFactory()
				.getMetamodel()
				.getTypeConfiguration()
				.getTypeDescriptorRegistryAccess();
		final SqlTypeDescriptor sqlTypeDescriptor = typeDescriptorRegistryAccess.getSqlTypeDescriptorRegistry()
				.getDescriptor( jdbcType );

		final JavaTypeDescriptor javaTypeDescriptor = sqlTypeDescriptor.getJdbcRecommendedJavaTypeMapping( typeDescriptorRegistryAccess );

		//  also add:
		session.getFactory().getMetamodel().getTypeConfiguration().getBasicTypeRegistry();

		switch ( jdbcType ) {
			case Types.BIGINT: {
				return javaTypeDescriptor.wrap( resultSet.getLong( position ), null );
			}
			case Types.BIT: {
				return javaTypeDescriptor.wrap( resultSet.getBoolean( position ), null );
			}
			case Types.BOOLEAN: {
				return javaTypeDescriptor.wrap( resultSet.getBoolean( position ), null );
			}
			case Types.CHAR: {
				return javaTypeDescriptor.wrap( resultSet.getString( position ), null );
			}
			case Types.DATE: {
				return javaTypeDescriptor.wrap( resultSet.getDate( position ), null );
			}
			case Types.DECIMAL: {
				return javaTypeDescriptor.wrap( resultSet.getBigDecimal( position ), null );
			}
			case Types.DOUBLE: {
				return javaTypeDescriptor.wrap( resultSet.getDouble( position ), null );
			}
			case Types.FLOAT: {
				return javaTypeDescriptor.wrap( resultSet.getFloat( position ), null );
			}
			case Types.INTEGER: {
				return javaTypeDescriptor.wrap( resultSet.getInt( position ), null );
			}
			case Types.LONGNVARCHAR: {
				return javaTypeDescriptor.wrap( resultSet.getString( position ), null );
			}
			case Types.LONGVARCHAR: {
				return javaTypeDescriptor.wrap( resultSet.getString( position ), null );
			}
			case Types.LONGVARBINARY: {
				return javaTypeDescriptor.wrap( resultSet.getBytes( position ), null );
			}
			case Types.NCHAR: {
				return javaTypeDescriptor.wrap( resultSet.getString( position ), null );
			}
			case Types.NUMERIC: {
				return javaTypeDescriptor.wrap( resultSet.getBigDecimal( position ), null );
			}
			case Types.NVARCHAR: {
				return javaTypeDescriptor.wrap( resultSet.getString( position ), null );
			}
			case Types.TIME: {
				return javaTypeDescriptor.wrap( resultSet.getTime( position ), null );
			}
			case Types.TIMESTAMP: {
				return javaTypeDescriptor.wrap( resultSet.getTimestamp( position ), null );
			}
			case Types.VARCHAR: {
				return javaTypeDescriptor.wrap( resultSet.getString( position ), null );
			}
		}

		throw new UnsupportedOperationException( "JDBC type [" + jdbcType + " not supported" );
	}
}
