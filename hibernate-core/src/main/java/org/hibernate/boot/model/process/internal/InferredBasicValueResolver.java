/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import java.util.function.Function;

import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class InferredBasicValueResolver<J> {
	private final TypeConfiguration typeConfiguration;

	private JavaTypeDescriptor<J> domainJtd;
	private JavaTypeDescriptor<?> relationalJtd;
	private SqlTypeDescriptor relationalStd;
	private BasicValueConverter valueConverter;
	private MutabilityPlan<J> mutabilityPlan;

	private InferredBasicValueResolution<J> resolution;

	public InferredBasicValueResolver(
			Function<TypeConfiguration,JavaTypeDescriptor<J>> explicitJtdAccess,
			Function<TypeConfiguration,SqlTypeDescriptor> explicitStdAccess,
			TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;

		this.domainJtd = explicitJtdAccess != null ? explicitJtdAccess.apply( typeConfiguration ) : null;
		this.relationalStd = explicitStdAccess != null ? explicitStdAccess.apply( typeConfiguration ) : null;
	}

	public BasicValue.Resolution<J> build() {
		if ( resolution == null ) {
			final BasicType<?> basicType = typeConfiguration.getBasicTypeRegistry().resolve( relationalJtd, relationalStd );

			//noinspection unchecked
			resolution = new InferredBasicValueResolution( basicType, domainJtd, relationalJtd, relationalStd, valueConverter, mutabilityPlan );
		}

		return resolution;
	}

	public void injectResolution(InferredBasicValueResolution<J> resolution) {
		this.resolution = resolution;
	}

	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	public JavaTypeDescriptor<?> getDomainJtd() {
		return domainJtd;
	}

	@SuppressWarnings("unchecked")
	public void setDomainJtd(JavaTypeDescriptor domainJtd) {
		this.domainJtd = domainJtd;
	}

	public JavaTypeDescriptor<?> getRelationalJtd() {
		return relationalJtd;
	}

	public void setRelationalJtd(JavaTypeDescriptor<?> relationalJtd) {
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
