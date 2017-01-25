/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.ast.select.SqlSelection;
import org.hibernate.sql.exec.results.process.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.exec.results.process.spi.SqlSelectionReader;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.MutabilityPlan;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.BasicTypeRegistry;
import org.hibernate.type.spi.ColumnMapping;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.VersionSupport;

/**
 * @author Steve Ebersole
 */
public class BasicTypeImpl<T> extends AbstractTypeImpl<T> implements BasicType<T>, SqlSelectionReader<T> {
	private final ColumnMapping columnMapping;
	private final BasicTypeRegistry.Key registryKey;
	private final JdbcLiteralFormatter jdbcLiteralFormatter;

	@SuppressWarnings("unchecked")
	public BasicTypeImpl(BasicJavaDescriptor javaDescriptor, ColumnMapping columnMapping) {
		this( javaDescriptor, javaDescriptor.getMutabilityPlan(), javaDescriptor.getComparator(), columnMapping );
	}

	@SuppressWarnings("unchecked")
	public BasicTypeImpl(BasicJavaDescriptor javaDescriptor, SqlTypeDescriptor sqlTypeDescriptor) {
		this( javaDescriptor, new ColumnMapping( sqlTypeDescriptor ) );
	}

	@SuppressWarnings("unchecked")
	public BasicTypeImpl(
			BasicJavaDescriptor javaDescriptor,
			MutabilityPlan mutabilityPlan,
			Comparator comparator,
			SqlTypeDescriptor sqlTypeDescriptor) {
		this( javaDescriptor, mutabilityPlan, comparator, new ColumnMapping( sqlTypeDescriptor ) );
	}

	@SuppressWarnings("unchecked")
	public BasicTypeImpl(
			BasicJavaDescriptor javaDescriptor,
			MutabilityPlan mutabilityPlan,
			Comparator comparator,
			ColumnMapping columnMapping) {
		super( javaDescriptor, mutabilityPlan, comparator );
		this.columnMapping = columnMapping;
		this.registryKey = BasicTypeRegistry.Key.from( javaDescriptor, columnMapping.getSqlTypeDescriptor() );
		this.jdbcLiteralFormatter = columnMapping.getSqlTypeDescriptor().getJdbcLiteralFormatter( getJavaTypeDescriptor() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public BasicJavaDescriptor<T> getJavaTypeDescriptor() {
		return (BasicJavaDescriptor) super.getJavaTypeDescriptor();
	}

	@Override
	public JdbcLiteralFormatter getJdbcLiteralFormatter() {
		return jdbcLiteralFormatter;
	}

	@Override
	public BasicTypeRegistry.Key getRegistryKey() {
		return registryKey;
	}

	@Override
	public Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public String asLoggableText() {
		return "BasicType(" + getTypeName() + ")";
	}

	@Override
	public ColumnMapping getColumnMapping() {
		return columnMapping;
	}


	@Override
	public VersionSupport<T> getVersionSupport() {
		return null;
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
		return getColumnMapping().getSqlTypeDescriptor().getExtractor( getJavaTypeDescriptor() ).extract(
				resultSet,
				sqlSelection.getJdbcResultSetIndex(),
				jdbcValuesSourceProcessingState.getPersistenceContext()
		);
	}

	@Override
	public T extractParameterValue(
			CallableStatement statement,
			int jdbcParameterIndex,
			SharedSessionContractImplementor session) throws SQLException {
		return getColumnMapping().getSqlTypeDescriptor().getExtractor( getJavaTypeDescriptor() ).extract(
				statement,
				jdbcParameterIndex,
				session
		);
	}

	@Override
	public T extractParameterValue(
			CallableStatement statement,
			String jdbcParameterName,
			SharedSessionContractImplementor session) throws SQLException {
		return getColumnMapping().getSqlTypeDescriptor().getExtractor( getJavaTypeDescriptor() ).extract(
				statement,
				jdbcParameterName,
				session
		);
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// legacy stuff, for now copied as-is

	@Override
	public Object hydrate(
			ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		return nullSafeGet(rs, names, session, owner);
	}

	@Override
	public Object assemble(
			Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return getMutabilityPlan().assemble( cached );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Serializable disassemble(
			T value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return getMutabilityPlan().disassemble( value );
	}

	@Override
	public boolean isDirty(T old, T current, SharedSessionContractImplementor session) {
		return !getJavaTypeDescriptor().areEqual( old, current );
	}

	@Override
	public boolean isDirty(
			T old,
			T current,
			boolean[] checkable,
			SharedSessionContractImplementor session) {
		return checkable[0] && !getJavaTypeDescriptor().areEqual( old, current );
	}

	@Override
	public boolean isModified(
			T dbState,
			T currentState,
			boolean[] checkable,
			SharedSessionContractImplementor session)
			throws HibernateException {
		return checkable[0] && isDirty( dbState, currentState, session );
	}

	@Override
	public int getHashCode(T value) throws HibernateException {
		return getJavaTypeDescriptor().extractHashCode( value );
	}

	@Override
	public int[] sqlTypes() throws MappingException {
		return new int[] { getColumnMapping().getSqlTypeDescriptor().getSqlType() };
	}

}
