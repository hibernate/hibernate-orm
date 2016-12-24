/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.process.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.sql.ast.select.SqlSelection;
import org.hibernate.sql.exec.results.process.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.exec.results.process.spi.SqlSelectionReader;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqlSelectionReaderImpl implements SqlSelectionReader {
	private final Reader reader;

	public SqlSelectionReaderImpl(int jdbcTypeCode) {
		reader = new JdbcTypeCodeReaderImpl( jdbcTypeCode );
	}

	public SqlSelectionReaderImpl(BasicType basicType) {
		reader = new BasicTypeReaderAdapterImpl( basicType );
	}

	public SqlSelectionReaderImpl(BasicType basicType, int jdbcTypeCode) {
		if ( basicType != null ) {
			reader = new BasicTypeReaderAdapterImpl( basicType );
		}
		else {
			reader = new JdbcTypeCodeReaderImpl( jdbcTypeCode );
		}
	}

	@Override
	public Object read(
			ResultSet resultSet,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			SqlSelection sqlSelection)
			throws SQLException {
		return reader.read( resultSet, jdbcValuesSourceProcessingState, sqlSelection.getJdbcResultSetIndex() );
	}

	private static interface Reader {
		Object read(
				ResultSet resultSet,
				JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
				int position)
				throws SQLException;
	}

	static class JdbcTypeCodeReaderImpl implements Reader {
		private final int jdbcTypeCode;

		public JdbcTypeCodeReaderImpl(int jdbcTypeCode) {
			this.jdbcTypeCode = jdbcTypeCode;
		}

		@Override
		public Object read(
				ResultSet resultSet,
				JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
				int position) throws SQLException {
			final SqlTypeDescriptor sqlDescriptor = jdbcValuesSourceProcessingState.getPersistenceContext()
					.getFactory()
					.getMetamodel()
					.getTypeConfiguration()
					.getTypeDescriptorRegistryAccess()
					.getSqlTypeDescriptorRegistry()
					.getDescriptor( jdbcTypeCode );

			final JavaTypeDescriptor javaTypeDescriptor = sqlDescriptor.getJdbcRecommendedJavaTypeMapping(
					jdbcValuesSourceProcessingState.getPersistenceContext().getFactory().getMetamodel().getTypeConfiguration()
			);

			return javaTypeDescriptor.wrap(
					extractJdbcValue( resultSet, jdbcTypeCode, position ),
					jdbcValuesSourceProcessingState.getPersistenceContext()
			);
		}
	}

	public static Object extractJdbcValue(ResultSet resultSet, int jdbcTypeCode, int position) throws SQLException {
		switch ( jdbcTypeCode ) {
			case Types.BIGINT: {
				return resultSet.getBigDecimal( position );
			}
			case Types.BIT: {
				return resultSet.getBoolean( position );
			}
			case Types.BOOLEAN: {
				return resultSet.getBoolean( position );
			}
			case Types.CHAR: {
				return resultSet.getString( position );
			}
			case Types.DATE: {
				return resultSet.getDate( position );
			}
			case Types.DECIMAL: {
				return resultSet.getBigDecimal( position );
			}
			case Types.DOUBLE: {
				return resultSet.getDouble( position );
			}
			case Types.FLOAT: {
				return resultSet.getFloat( position );
			}
			case Types.INTEGER: {
				return resultSet.getInt( position );
			}
			case Types.LONGNVARCHAR: {
				return resultSet.getNString( position );
			}
			case Types.LONGVARCHAR: {
				return resultSet.getString( position );
			}
			case Types.LONGVARBINARY: {
				return resultSet.getBytes( position );
			}
			case Types.NCHAR: {
				return resultSet.getNString( position );
			}
			case Types.NUMERIC: {
				return resultSet.getBigDecimal( position );
			}
			case Types.NVARCHAR: {
				return resultSet.getNString( position );
			}
			case Types.TIME: {
				return resultSet.getTime( position );
			}
			case Types.TIMESTAMP: {
				return resultSet.getTimestamp( position );
			}
			case Types.VARCHAR: {
				return resultSet.getString( position );
			}
			case Types.BLOB: {
				return resultSet.getBlob( position );
			}
			case Types.CLOB: {
				return resultSet.getClob( position );
			}
			case Types.NCLOB: {
				return resultSet.getNClob( position );
			}
		}

		throw new UnsupportedOperationException( "JDBC type [" + jdbcTypeCode + " not supported" );
	}

	private class BasicTypeReaderAdapterImpl implements Reader {
		private final BasicType basicType;

		public BasicTypeReaderAdapterImpl(BasicType basicType) {
			this.basicType = basicType;
		}

		@Override
		public Object read(
				ResultSet resultSet,
				JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
				int position) throws SQLException {
			// Any more than a single column is an error at this level
			final int jdbcTypeCode = basicType.sqlTypes()[0];

			final JavaTypeDescriptor javaTypeDescriptor = jdbcValuesSourceProcessingState.getPersistenceContext()
					.getFactory()
					.getMetamodel()
					.getTypeConfiguration()
					.getTypeDescriptorRegistryAccess()
					.getJavaTypeDescriptorRegistry()
					.getDescriptor( basicType.getReturnedClass() );

			return javaTypeDescriptor.wrap(
					extractJdbcValue( resultSet, jdbcTypeCode, position ),
					jdbcValuesSourceProcessingState.getPersistenceContext()
			);
		}
	}
}
