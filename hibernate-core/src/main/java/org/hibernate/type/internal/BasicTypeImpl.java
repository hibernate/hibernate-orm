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

import org.hibernate.metamodel.model.domain.spi.VersionSupport;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionReader;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.ValueBinder;
import org.hibernate.type.descriptor.spi.ValueExtractor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class BasicTypeImpl<T> implements BasicType<T>, SqlSelectionReader<T> {
	private final BasicJavaDescriptor javaDescriptor;
	private final SqlTypeDescriptor sqlTypeDescriptor;

	private VersionSupport<T> versionSupport;

	@SuppressWarnings("unchecked")
	public BasicTypeImpl(BasicJavaDescriptor javaDescriptor, SqlTypeDescriptor sqlTypeDescriptor) {
		this.javaDescriptor = javaDescriptor;
		this.sqlTypeDescriptor = sqlTypeDescriptor;

		this.versionSupport = javaDescriptor.getVersionSupport();
	}

	public BasicTypeImpl setVersionSupport(VersionSupport<T> versionSupport){
		// todo (6.0) : not sure this is the best place to define this...
		// 		the purpose of this is to account for cases where the proper
		//		VersionSupport to use is not the same as the JTD's
		//		VersionSupport.  This only(?) happens when we have a
		//		`byte[]` mapped to T-SQL ROWVERSION/TIMESTAMP data-type -
		//		which is represented as a `byte[]`, but with a very
		//		specific comparison algorithm.
		//
		//		the alternative is to handle this distinction when building
		//		the VersionDescriptor - if the JTD is a `byte[]`, we'd use
		//		a specialized VersionSupport
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
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return sqlTypeDescriptor;
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
	@SuppressWarnings("unchecked")
	public T read(
			ResultSet resultSet,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			SqlSelection sqlSelection) throws SQLException {
		return getValueExtractor().extract(
				resultSet,
				sqlSelection.getJdbcResultSetIndex(),
				jdbcValuesSourceProcessingState.getPersistenceContext()
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public T extractParameterValue(
			CallableStatement statement,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			int jdbcParameterIndex) throws SQLException {
		return getValueExtractor().extract(
				statement,
				jdbcParameterIndex,
				jdbcValuesSourceProcessingState.getPersistenceContext()
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public T extractParameterValue(
			CallableStatement statement,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			String jdbcParameterName) throws SQLException {
		return getValueExtractor().extract(
				statement,
				jdbcParameterName,
				jdbcValuesSourceProcessingState.getPersistenceContext()
		);
	}

	@Override
	public ValueBinder getValueBinder() {
		return getSqlTypeDescriptor().getBinder( getJavaTypeDescriptor() );
	}

	@Override
	public ValueExtractor<T> getValueExtractor() {
		return getSqlTypeDescriptor().getExtractor( getJavaTypeDescriptor() );
	}
}
