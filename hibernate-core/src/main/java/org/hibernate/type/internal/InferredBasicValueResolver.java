/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.util.function.Function;

import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class InferredBasicValueResolver<J> {
	private final TypeConfiguration typeConfiguration;

	private BasicJavaDescriptor<J> domainJtd;
	private BasicJavaDescriptor<?> relationalJtd;
	private SqlTypeDescriptor relationalStd;
	private BasicValueConverter valueConverter;
	private MutabilityPlan<J> mutabilityPlan;

	public InferredBasicValueResolver(
			Function<TypeConfiguration,BasicJavaDescriptor<J>> explicitJtdAccess,
			Function<TypeConfiguration,SqlTypeDescriptor> explicitStdAccess,
			TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;

		this.domainJtd = explicitJtdAccess != null ? explicitJtdAccess.apply( typeConfiguration ) : null;
		this.relationalStd = explicitStdAccess != null ? explicitStdAccess.apply( typeConfiguration ) : null;
	}

	public BasicValueMapping.Resolution<J> build() {
		final BasicTypeImpl<J> basicType = new BasicTypeImpl<>(
				relationalJtd,
				relationalStd,
				relationalStd.getSqlExpressableType( relationalJtd, typeConfiguration )
		);

		return new InferredBasicValueResolution<>( basicType, domainJtd, valueConverter, mutabilityPlan );
	}

	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	public BasicJavaDescriptor<?> getDomainJtd() {
		return domainJtd;
	}

	@SuppressWarnings("unchecked")
	public void setDomainJtd(BasicJavaDescriptor domainJtd) {
		this.domainJtd = domainJtd;
	}

	public BasicJavaDescriptor<?> getRelationalJtd() {
		return relationalJtd;
	}

	public void setRelationalJtd(BasicJavaDescriptor<?> relationalJtd) {
		this.relationalJtd = relationalJtd;
	}

	public SqlTypeDescriptor getRelationalStd() {
		return relationalStd;
	}

	public void setRelationalStd(SqlTypeDescriptor relationalStd) {
		this.relationalStd = relationalStd;
	}

	public BasicValueConverter getValueConverter() {
		return valueConverter;
	}

	public void setValueConverter(BasicValueConverter valueConverter) {
		this.valueConverter = valueConverter;
	}

	public MutabilityPlan getMutabilityPlan() {
		return mutabilityPlan;
	}

	@SuppressWarnings("unchecked")
	public void setMutabilityPlan(MutabilityPlan mutabilityPlan) {
		this.mutabilityPlan = mutabilityPlan;
	}
}
