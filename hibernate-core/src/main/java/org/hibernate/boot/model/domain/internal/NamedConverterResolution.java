/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain.internal;

import java.util.function.Function;
import javax.persistence.AttributeConverter;

import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.boot.model.domain.ResolutionContext;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.metamodel.model.domain.spi.BasicValueMapper;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.SqlTypeDescriptorIndicators;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class NamedConverterResolution<J> implements BasicValueMapping.Resolution<J>, BasicValueMapper<J> {

	public static NamedConverterResolution from(
			String name,
			Function<TypeConfiguration,BasicJavaDescriptor<?>> explicitJtdAccess,
			Function<TypeConfiguration,SqlTypeDescriptor> explicitStdAccess,
			JpaAttributeConverterCreationContext converterCreationContext,
			MutabilityPlan explicitMutabilityPlan,
			SqlTypeDescriptorIndicators sqlTypeIndicators,
			ResolutionContext resolutionContext) {
		assert name.startsWith( ConverterDescriptor.TYPE_NAME_PREFIX );
		final String converterClassName = name.substring( ConverterDescriptor.TYPE_NAME_PREFIX.length() );

		final StandardServiceRegistry serviceRegistry = resolutionContext.getBootstrapContext().getServiceRegistry();
		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

		final Class<? extends AttributeConverter> converterClass = classLoaderService.classForName( converterClassName );
		final ClassBasedConverterDescriptor converterDescriptor = new ClassBasedConverterDescriptor(
				converterClass,
				resolutionContext.getBootstrapContext().getClassmateContext()
		);
		final JpaAttributeConverter converter = converterDescriptor.createJpaAttributeConverter( converterCreationContext );

		final BasicJavaDescriptor explicitJtd = explicitJtdAccess != null
				? explicitJtdAccess.apply( resolutionContext.getBootstrapContext().getTypeConfiguration() )
				: null;
		final BasicJavaDescriptor domainJtd = explicitJtdAccess != null
				? explicitJtd
				: converter.getDomainJavaDescriptor();

		final SqlTypeDescriptor explicitStd = explicitStdAccess != null
				? explicitStdAccess.apply( resolutionContext.getBootstrapContext().getTypeConfiguration() )
				: null;
		final BasicJavaDescriptor relationalJtd = converter.getRelationalJavaDescriptor();
		final SqlTypeDescriptor relationalStd = explicitStd != null
				? explicitStd
				: relationalJtd.getJdbcRecommendedSqlType( sqlTypeIndicators );

		return new NamedConverterResolution(
				name,
				domainJtd,
				new BasicTypeImpl<>(
						relationalJtd,
						relationalStd,
						relationalStd.getSqlExpressableType( relationalJtd, resolutionContext.getBootstrapContext().getTypeConfiguration() )
				),
				converter,
				explicitMutabilityPlan != null
						? explicitMutabilityPlan
						: converter.getDomainJavaDescriptor().getMutabilityPlan()
		);
	}

	private final String name;

	private final BasicJavaDescriptor domainJtd;

	private final BasicType basicType;

	private final BasicValueConverter valueConverter;
	private final MutabilityPlan mutabilityPlan;

	private NamedConverterResolution(
			String name,
			BasicJavaDescriptor domainJtd,
			BasicType basicType,
			JpaAttributeConverter valueConverter,
			MutabilityPlan mutabilityPlan) {
		this.name = name;
		this.domainJtd = domainJtd;
		this.basicType = basicType;
		this.valueConverter = valueConverter;
		this.mutabilityPlan = mutabilityPlan;

	}

	@Override
	public BasicType getBasicType() {
		return basicType;
	}

	@Override
	public BasicValueMapper<J> getValueMapper() {
		return this;
	}

	@Override
	public BasicJavaDescriptor<J> getDomainJavaDescriptor() {
		return domainJtd;
	}

	@Override
	public BasicJavaDescriptor<?> getRelationalJavaDescriptor() {
		return basicType.getJavaTypeDescriptor();
	}

	@Override
	public SqlTypeDescriptor getRelationalSqlTypeDescriptor() {
		return basicType.getSqlTypeDescriptor();
	}

	@Override
	public BasicValueConverter getValueConverter() {
		return valueConverter;
	}

	@Override
	public MutabilityPlan<J> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return basicType.getSqlExpressableType();
	}

	@Override
	public String toString() {
		return "NamedConverterResolution(" + name + ')';
	}
}
