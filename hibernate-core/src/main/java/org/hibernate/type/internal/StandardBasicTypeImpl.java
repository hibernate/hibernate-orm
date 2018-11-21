/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.util.function.Consumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.StandardSqlExpressableTypeImpl;
import org.hibernate.type.spi.StandardSpiBasicTypes.StandardBasicType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class StandardBasicTypeImpl<J> implements StandardBasicType<J> {
	private final BasicJavaDescriptor domainJtd;
	private final BasicJavaDescriptor relationalJtd;
	private final SqlTypeDescriptor relationalStd;
	private final BasicValueConverter valueConverter;
	private final MutabilityPlan mutabilityPlan;

	public StandardBasicTypeImpl(BasicJavaDescriptor<J> jtd, SqlTypeDescriptor std) {
		this( jtd, jtd, std, null, jtd.getMutabilityPlan() );
	}

	public StandardBasicTypeImpl(
			BasicJavaDescriptor<J> jtd,
			SqlTypeDescriptor std,
			MutabilityPlan<J> mutabilityPlan) {
		this( jtd, jtd, std, null, jtd.getMutabilityPlan() );
	}

	public StandardBasicTypeImpl(
			BasicJavaDescriptor<J> domainJtd,
			BasicJavaDescriptor<?> relationalJtd,
			SqlTypeDescriptor std,
			BasicValueConverter<J, ?> valueConverter) {
		this( domainJtd, relationalJtd, std, valueConverter, null );
	}

	public StandardBasicTypeImpl(
			BasicJavaDescriptor domainJtd,
			BasicJavaDescriptor relationalJtd,
			SqlTypeDescriptor relationalStd,
			BasicValueConverter valueConverter,
			MutabilityPlan mutabilityPlan) {
		this.domainJtd = domainJtd;
		this.relationalJtd = relationalJtd;
		this.relationalStd = relationalStd;
		this.valueConverter = valueConverter;
		this.mutabilityPlan = mutabilityPlan;
	}

	@Override
	public BasicJavaDescriptor<J> getDomainJavaTypeDescriptor() {
		return domainJtd;
	}

	@Override
	public BasicJavaDescriptor getRelationalJavaTypeDescriptor() {
		return relationalJtd;
	}

	@Override
	public SqlTypeDescriptor getRelationalSqlTypeDescriptor() {
		return relationalStd;
	}

	@Override
	public BasicValueConverter<J, ?> getValueConverter() {
		return valueConverter;
	}

	@Override
	public MutabilityPlan<J> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public BasicJavaDescriptor<J> getJavaTypeDescriptor() {
		return getDomainJavaTypeDescriptor();
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return relationalStd;
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return null;
	}

	@Override
	public Object unresolve(Object value, SharedSessionContractImplementor session) {
		Object result = value;
		if ( valueConverter != null ) {
			result = valueConverter.toRelationalValue( value, session );
		}

		return result;
	}

	@Override
	public void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
		jdbcValueCollector.collect(
				value,
				resolveJdbcMapping( session.getFactory().getTypeConfiguration() ),
				// these static StandardBasicType references are not mapped
				// to a specific Column the way a Navigable would
				null
		);
	}

	private SqlExpressableType resolveJdbcMapping(TypeConfiguration typeConfiguration) {
		return typeConfiguration.resolveStandardBasicType( this ).getSqlExpressableType( typeConfiguration );
	}

	@Override
	public void visitJdbcTypes(
			Consumer<SqlExpressableType> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		action.accept( resolveJdbcMapping( typeConfiguration ) );
	}

	@Override
	public int getNumberOfJdbcParametersNeeded() {
		return 1;
	}
}
