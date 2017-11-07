/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.results.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.metamodel.model.domain.spi.ConvertibleNavigable;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.spi.SqlSelectionReader;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqlSelectionReaderImpl implements SqlSelectionReader {
	private final Reader reader;

	public SqlSelectionReaderImpl(int jdbcTypeCode) {
		reader = new JdbcTypeCodeReaderImpl( jdbcTypeCode );
	}

	public SqlSelectionReaderImpl(BasicValuedExpressableType expressableType) {
		reader = new BasicTypeReaderAdapterImpl( expressableType );
	}

	public SqlSelectionReaderImpl(BasicValuedExpressableType basicType, int jdbcTypeCode) {
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
			SqlSelection sqlSelection) throws SQLException {
		return reader.read( resultSet, jdbcValuesSourceProcessingState, sqlSelection.getJdbcResultSetIndex() );
	}

	@Override
	public Object extractParameterValue(
			CallableStatement statement,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			int jdbcParameterIndex) throws SQLException {
		return reader.readParameterValue( statement, jdbcValuesSourceProcessingState, jdbcParameterIndex );
	}

	@Override
	public Object extractParameterValue(
			CallableStatement statement,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			String jdbcParameterName) throws SQLException {
		return reader.readParameterValue( statement, jdbcValuesSourceProcessingState, jdbcParameterName );
	}

	private static interface Reader {
		<T> T read(
				ResultSet resultSet,
				JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
				int position) throws SQLException;

		<T> T readParameterValue(
				CallableStatement statement,
				JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
				int jdbcParameterIndex) throws SQLException;

		<T> T readParameterValue(
				CallableStatement statement,
				JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
				String jdbcParameterName) throws SQLException;
	}

	static class JdbcTypeCodeReaderImpl implements Reader {
		private final int jdbcTypeCode;

		public JdbcTypeCodeReaderImpl(int jdbcTypeCode) {
			this.jdbcTypeCode = jdbcTypeCode;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object read(
				ResultSet resultSet,
				JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
				int position) throws SQLException {
			// todo (6.0) - we should cache the SqlTypeDescriptor in the ctor...
			final SqlTypeDescriptor sqlDescriptor = jdbcValuesSourceProcessingState.getPersistenceContext()
					.getFactory()
					.getMetamodel()
					.getTypeConfiguration()
					.getSqlTypeDescriptorRegistry()
					.getDescriptor( jdbcTypeCode );

			final JavaTypeDescriptor javaTypeDescriptor = sqlDescriptor.getJdbcRecommendedJavaTypeMapping(
					jdbcValuesSourceProcessingState.getPersistenceContext().getFactory().getMetamodel().getTypeConfiguration()
			);

			return extractRawJdbcValue(
					resultSet,
					(BasicJavaDescriptor) javaTypeDescriptor,
					sqlDescriptor,
					jdbcValuesSourceProcessingState,
					position
			);
		}

		@Override
		public <T> T readParameterValue(
				CallableStatement statement,
				JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
				int jdbcParameterIndex) throws SQLException {
			final SqlTypeDescriptor sqlDescriptor = jdbcValuesSourceProcessingState.getPersistenceContext()
					.getFactory()
					.getMetamodel()
					.getTypeConfiguration()
					.getSqlTypeDescriptorRegistry()
					.getDescriptor( jdbcTypeCode );

			final JavaTypeDescriptor<T> javaTypeDescriptor = sqlDescriptor.getJdbcRecommendedJavaTypeMapping(
					jdbcValuesSourceProcessingState.getPersistenceContext().getFactory().getMetamodel().getTypeConfiguration()
			);

			return extractRawJdbcParameterValue(
					statement,
					(BasicJavaDescriptor<T>) javaTypeDescriptor,
					sqlDescriptor,
					jdbcValuesSourceProcessingState,
					jdbcParameterIndex
			);
		}

		@Override
		public <T> T readParameterValue(
				CallableStatement statement,
				JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
				String jdbcParameterName) throws SQLException {
			final SqlTypeDescriptor sqlDescriptor = jdbcValuesSourceProcessingState.getPersistenceContext()
					.getFactory()
					.getMetamodel()
					.getTypeConfiguration()
					.getSqlTypeDescriptorRegistry()
					.getDescriptor( jdbcTypeCode );

			final JavaTypeDescriptor<T> javaTypeDescriptor = sqlDescriptor.getJdbcRecommendedJavaTypeMapping(
					jdbcValuesSourceProcessingState.getPersistenceContext().getFactory().getMetamodel().getTypeConfiguration()
			);

			return extractRawJdbcParameterValue(
					statement,
					(BasicJavaDescriptor<T>) javaTypeDescriptor,
					sqlDescriptor,
					jdbcValuesSourceProcessingState,
					jdbcParameterName
			);
		}
	}

	private static <T> T extractRawJdbcValue(
			ResultSet resultSet,
			BasicJavaDescriptor<T> javaTypeDescriptor,
			SqlTypeDescriptor sqlTypeDescriptor,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			int position) throws SQLException {
		assert resultSet != null;
		assert javaTypeDescriptor != null;
		assert sqlTypeDescriptor != null;
		assert jdbcValuesSourceProcessingState != null;
		assert position > 0;

		return sqlTypeDescriptor.getExtractor( javaTypeDescriptor ).extract(
				resultSet,
				position,
				jdbcValuesSourceProcessingState.getPersistenceContext()
		);
	}

	private static <T> T extractRawJdbcParameterValue(
			CallableStatement statement,
			BasicJavaDescriptor<T> javaTypeDescriptor,
			SqlTypeDescriptor sqlTypeDescriptor,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			int jdbcParameterIndex) throws SQLException {
		assert statement != null;
		assert javaTypeDescriptor != null;
		assert sqlTypeDescriptor != null;
		assert jdbcValuesSourceProcessingState != null;

		return sqlTypeDescriptor.getExtractor( javaTypeDescriptor ).extract(
				statement,
				jdbcParameterIndex,
				jdbcValuesSourceProcessingState.getPersistenceContext()
		);
	}

	private static <T> T extractRawJdbcParameterValue(
			CallableStatement statement,
			BasicJavaDescriptor<T> javaTypeDescriptor,
			SqlTypeDescriptor sqlTypeDescriptor,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			String jdbcParameterName) throws SQLException {
		assert statement != null;
		assert javaTypeDescriptor != null;
		assert sqlTypeDescriptor != null;
		assert jdbcValuesSourceProcessingState != null;

		return sqlTypeDescriptor.getExtractor( javaTypeDescriptor ).extract(
				statement,
				jdbcParameterName,
				jdbcValuesSourceProcessingState.getPersistenceContext()
		);
	}

	private class BasicTypeReaderAdapterImpl implements Reader {
		private final BasicValuedExpressableType expressableType;

		// todo (6.0) : it is not generally true that the raw JDBC value for a converted Navigable is the same Java type as the Navigable's type.
		//		- in fact it is generally this use case that AttributeConverter tries to address.

		public BasicTypeReaderAdapterImpl(BasicValuedExpressableType expressableType) {
			this.expressableType = expressableType;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object read(
				ResultSet resultSet,
				JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
				int position) throws SQLException {
			return extractRawJdbcValue(
					resultSet,
					determineJavaTypeDescriptor( expressableType ),
					expressableType.getBasicType().getSqlTypeDescriptor(),
					jdbcValuesSourceProcessingState,
					position
			);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T readParameterValue(
				CallableStatement statement,
				JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
				int jdbcParameterIndex) throws SQLException {
			return extractRawJdbcParameterValue(
					statement,
					determineJavaTypeDescriptor( expressableType ),
					expressableType.getBasicType().getSqlTypeDescriptor(),
					jdbcValuesSourceProcessingState,
					jdbcParameterIndex
			);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T readParameterValue(
				CallableStatement statement,
				JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
				String jdbcParameterName) throws SQLException {
			return extractRawJdbcParameterValue(
					statement,
					determineJavaTypeDescriptor( expressableType ),
					expressableType.getBasicType().getSqlTypeDescriptor(),
					jdbcValuesSourceProcessingState,
					jdbcParameterName
			);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> BasicJavaDescriptor<T> determineJavaTypeDescriptor(BasicValuedExpressableType expressableType) {
		if ( expressableType instanceof ConvertibleNavigable
				&& ( (ConvertibleNavigable) expressableType ).getAttributeConverterDefinition() != null ) {
			return ( (ConvertibleNavigable) expressableType ).getAttributeConverterDefinition().getJdbcType();
		}

		return expressableType.getJavaTypeDescriptor();
	}

}
