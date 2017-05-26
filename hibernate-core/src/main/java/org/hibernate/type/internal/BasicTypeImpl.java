/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.sql.ast.consume.results.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionReader;
import org.hibernate.sql.ast.tree.spi.select.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.ColumnDescriptor;
import org.hibernate.metamodel.model.domain.spi.VersionSupport;

/**
 * @author Steve Ebersole
 */
public class BasicTypeImpl<T> implements BasicType<T>, SqlSelectionReader<T> {
	private final BasicJavaDescriptor javaDescriptor;
	private final ColumnDescriptor columnMapping;
	private VersionSupport<T> versionSupport;

	@SuppressWarnings("unchecked")
	public BasicTypeImpl(BasicJavaDescriptor javaDescriptor, SqlTypeDescriptor sqlTypeDescriptor) {
		this( javaDescriptor, new ColumnDescriptor( sqlTypeDescriptor ) );
	}

	@SuppressWarnings("unchecked")
	public BasicTypeImpl(BasicJavaDescriptor javaDescriptor, ColumnDescriptor columnMapping) {
		this.javaDescriptor = javaDescriptor;
		this.columnMapping = columnMapping;
		this.versionSupport = javaDescriptor.getVersionSupport();
	}

	public BasicTypeImpl setVersionSupport(VersionSupport<T> versionSupport){
		this.versionSupport = versionSupport;
		return this;
	}

	@Override
	public BasicType<T> getBasicType() {
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public BasicJavaDescriptor<T> getJavaTypeDescriptor() {
		return javaDescriptor;
	}

	@Override
	public boolean areEqual(T x, T y) throws HibernateException {
		return EqualsHelper.areEqual( x, y );
	}

	@Override
	public Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public ColumnDescriptor getColumnDescriptor() {
		return columnMapping;
	}


	@Override
	public Optional<VersionSupport<T>> getVersionSupport() {
		return Optional.ofNullable( versionSupport );
	}

	@Override
	public SqlSelectionReader<T> getSqlSelectionReader() {
		return this;
	}

	@Override
	public T read(
			ResultSet resultSet,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			SqlSelection sqlSelection) throws SQLException {
		return getColumnDescriptor().getSqlTypeDescriptor().getExtractor( getJavaTypeDescriptor() ).extract(
				resultSet,
				sqlSelection.getJdbcResultSetIndex(),
				jdbcValuesSourceProcessingState.getPersistenceContext()
		);
	}

	@Override
	public T extractParameterValue(
			CallableStatement statement,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			int jdbcParameterIndex) throws SQLException {
		return getColumnDescriptor().getSqlTypeDescriptor().getExtractor( getJavaTypeDescriptor() ).extract(
				statement,
				jdbcParameterIndex,
				jdbcValuesSourceProcessingState.getPersistenceContext()
		);
	}

	@Override
	public T extractParameterValue(
			CallableStatement statement,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			String jdbcParameterName) throws SQLException {
		return getColumnDescriptor().getSqlTypeDescriptor().getExtractor( getJavaTypeDescriptor() ).extract(
				statement,
				jdbcParameterName,
				jdbcValuesSourceProcessingState.getPersistenceContext()
		);
	}

}
