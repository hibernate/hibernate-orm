/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import java.util.function.Function;
import javax.persistence.AttributeConverter;

import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptorIndicators;
import org.hibernate.type.internal.StandardBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class NamedConverterResolution<J> implements BasicValue.Resolution<J> {

	public static NamedConverterResolution from(
			String name,
			Function<TypeConfiguration, JavaTypeDescriptor<?>> explicitJtdAccess,
			Function<TypeConfiguration, SqlTypeDescriptor> explicitStdAccess,
			JpaAttributeConverterCreationContext converterCreationContext,
			MutabilityPlan explicitMutabilityPlan,
			SqlTypeDescriptorIndicators sqlTypeIndicators,
			MetadataBuildingContext context) {
		assert name.startsWith( ConverterDescriptor.TYPE_NAME_PREFIX );
		final String converterClassName = name.substring( ConverterDescriptor.TYPE_NAME_PREFIX.length() );

		final StandardServiceRegistry serviceRegistry = context.getBootstrapContext().getServiceRegistry();
		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

		final Class<? extends AttributeConverter> converterClass = classLoaderService.classForName( converterClassName );
		final ClassBasedConverterDescriptor converterDescriptor = new ClassBasedConverterDescriptor(
				converterClass,
				context.getBootstrapContext().getClassmateContext()
		);
		final JpaAttributeConverter converter = converterDescriptor.createJpaAttributeConverter( converterCreationContext );

		final JavaTypeDescriptor explicitJtd = explicitJtdAccess != null
				? explicitJtdAccess.apply( context.getBootstrapContext().getTypeConfiguration() )
				: null;
		final JavaTypeDescriptor domainJtd = explicitJtd != null
				? explicitJtd
				: converter.getDomainJavaDescriptor();

		final SqlTypeDescriptor explicitStd = explicitStdAccess != null
				? explicitStdAccess.apply( context.getBootstrapContext().getTypeConfiguration() )
				: null;
		final JavaTypeDescriptor relationalJtd = converter.getRelationalJavaDescriptor();
		final SqlTypeDescriptor relationalStd = explicitStd != null
				? explicitStd
				: relationalJtd.getJdbcRecommendedSqlType( sqlTypeIndicators );

		//noinspection unchecked
		return new NamedConverterResolution(
				name,
				new StandardBasicTypeImpl( relationalJtd, relationalStd ),
				converter,
				explicitMutabilityPlan != null
						? explicitMutabilityPlan
						: converter.getDomainJavaDescriptor().getMutabilityPlan()
		);
	}

	private final String name;

	private final BasicType basicType;

	private final BasicValueConverter valueConverter;
	private final MutabilityPlan mutabilityPlan;

	private NamedConverterResolution(
			String name,
			BasicType basicType,
			JpaAttributeConverter valueConverter,
			MutabilityPlan mutabilityPlan) {
		this.name = name;
		this.basicType = basicType;
		this.valueConverter = valueConverter;
		this.mutabilityPlan = mutabilityPlan;

	}

	@Override
	public BasicType getResolvedBasicType() {
		return basicType;
	}

	@Override
	public JavaTypeDescriptor<J> getDomainJavaDescriptor() {
		//noinspection unchecked
		return valueConverter.getDomainJavaDescriptor();
	}

	@Override
	public JavaTypeDescriptor<?> getRelationalJavaDescriptor() {
		return valueConverter.getRelationalJavaDescriptor();
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
	public String toString() {
		return "NamedConverterResolution(" + name + ')';
	}
}
