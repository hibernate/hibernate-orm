/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import java.util.function.Function;
import jakarta.persistence.AttributeConverter;

import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.converter.AttributeConverterMutabilityPlanImpl;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("rawtypes")
public class NamedConverterResolution<J> implements BasicValue.Resolution<J> {

	public static NamedConverterResolution from(
			ConverterDescriptor converterDescriptor,
			Function<TypeConfiguration, BasicJavaType> explicitJtdAccess,
			Function<TypeConfiguration, JdbcTypeDescriptor> explicitStdAccess,
			Function<TypeConfiguration, MutabilityPlan> explicitMutabilityPlanAccess,
			JdbcTypeDescriptorIndicators sqlTypeIndicators,
			JpaAttributeConverterCreationContext converterCreationContext,
			MetadataBuildingContext context) {
		return fromInternal(
				explicitJtdAccess,
				explicitStdAccess,
				explicitMutabilityPlanAccess,
				converterDescriptor.createJpaAttributeConverter( converterCreationContext ),
				sqlTypeIndicators,
				context
		);
	}

	public static NamedConverterResolution from(
			String name,
			Function<TypeConfiguration, BasicJavaType> explicitJtdAccess,
			Function<TypeConfiguration, JdbcTypeDescriptor> explicitStdAccess,
			Function<TypeConfiguration, MutabilityPlan> explicitMutabilityPlanAccess,
			JdbcTypeDescriptorIndicators sqlTypeIndicators,
			JpaAttributeConverterCreationContext converterCreationContext,
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

		return fromInternal(
				explicitJtdAccess,
				explicitStdAccess,
				explicitMutabilityPlanAccess,
				converterDescriptor.createJpaAttributeConverter( converterCreationContext ),
				sqlTypeIndicators,
				context
		);
	}

	private static NamedConverterResolution fromInternal(
			Function<TypeConfiguration, BasicJavaType> explicitJtdAccess,
			Function<TypeConfiguration, JdbcTypeDescriptor> explicitStdAccess,
			Function<TypeConfiguration, MutabilityPlan> explicitMutabilityPlanAccess,
			JpaAttributeConverter converter, JdbcTypeDescriptorIndicators sqlTypeIndicators,
			MetadataBuildingContext context) {
		final TypeConfiguration typeConfiguration = context.getBootstrapContext().getTypeConfiguration();

		final JavaType explicitJtd = explicitJtdAccess != null
				? explicitJtdAccess.apply( typeConfiguration )
				: null;

		final JavaType domainJtd = explicitJtd != null
				? explicitJtd
				: converter.getDomainJavaDescriptor();

		final JdbcTypeDescriptor explicitJdbcType = explicitStdAccess != null
				? explicitStdAccess.apply( typeConfiguration )
				: null;

		final JavaType relationalJtd = converter.getRelationalJavaDescriptor();

		final JdbcTypeDescriptor jdbcType = explicitJdbcType != null
				? explicitJdbcType
				: relationalJtd.getRecommendedJdbcType( sqlTypeIndicators );

		final MutabilityPlan explicitMutabilityPlan = explicitMutabilityPlanAccess != null
				? explicitMutabilityPlanAccess.apply( typeConfiguration )
				: null;


		final MutabilityPlan mutabilityPlan;
		if ( explicitMutabilityPlan != null ) {
			mutabilityPlan = explicitMutabilityPlan;
		}
		else if ( ! domainJtd.getMutabilityPlan().isMutable() ) {
			mutabilityPlan = ImmutableMutabilityPlan.INSTANCE;
		}
		else {
			mutabilityPlan = new AttributeConverterMutabilityPlanImpl( converter, true );
		}

		return new NamedConverterResolution(
				domainJtd,
				relationalJtd,
				jdbcType,
				converter,
				mutabilityPlan,
				context.getBootstrapContext().getTypeConfiguration()
		);
	}


	private final JavaType domainJtd;
	private final JavaType relationalJtd;
	private final JdbcTypeDescriptor jdbcTypeDescriptor;

	private final JpaAttributeConverter valueConverter;
	private final MutabilityPlan mutabilityPlan;

	private final JdbcMapping jdbcMapping;

	private final BasicType legacyResolvedType;

	@SuppressWarnings("unchecked")
	public NamedConverterResolution(
			JavaType domainJtd,
			JavaType relationalJtd,
			JdbcTypeDescriptor jdbcTypeDescriptor,
			JpaAttributeConverter valueConverter,
			MutabilityPlan mutabilityPlan,
			TypeConfiguration typeConfiguration) {
		assert domainJtd != null;
		this.domainJtd = domainJtd;

		assert relationalJtd != null;
		this.relationalJtd = relationalJtd;

		assert jdbcTypeDescriptor != null;
		this.jdbcTypeDescriptor = jdbcTypeDescriptor;

		assert valueConverter != null;
		this.valueConverter = valueConverter;

		assert mutabilityPlan != null;
		this.mutabilityPlan = mutabilityPlan;

		this.jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve(
				relationalJtd,
				jdbcTypeDescriptor
		);
//		this.jdbcMapping = new JdbcMapping() {
//			private final ValueExtractor extractor = relationalStd.getExtractor( relationalJtd );
//			private final ValueBinder binder = relationalStd.getBinder( relationalJtd );
//
//			@Override
//			public JavaTypeDescriptor getJavaTypeDescriptor() {
//				return relationalJtd;
//			}
//
//			@Override
//			public SqlTypeDescriptor getSqlTypeDescriptor() {
//				return relationalStd;
//			}
//
//			@Override
//			public ValueExtractor getJdbcValueExtractor() {
//				return extractor;
//			}
//
//			@Override
//			public ValueBinder getJdbcValueBinder() {
//				return binder;
//			}
//		};

//		this.jdbcMapping = new ConverterJdbcMappingImpl(
//				domainJtd,
//				relationalJtd,
//				relationalStd,
//				valueConverter,
//				mutabilityPlan,
//				typeConfiguration
//		);

		this.legacyResolvedType = new AttributeConverterTypeAdapter(
				ConverterDescriptor.TYPE_NAME_PREFIX + valueConverter.getConverterJavaTypeDescriptor().getJavaType().getTypeName(),
				String.format(
						"BasicType adapter for AttributeConverter<%s,%s>",
						domainJtd.getJavaType().getTypeName(),
						relationalJtd.getJavaType().getTypeName()
				),
				valueConverter,
				jdbcTypeDescriptor,
				relationalJtd,
				domainJtd,
				mutabilityPlan
		);
	}

	@Override
	public BasicType<J> getLegacyResolvedBasicType() {
		//noinspection unchecked
		return legacyResolvedType;
	}

	@Override
	public JavaType<J> getDomainJavaDescriptor() {
		//noinspection unchecked
		return domainJtd;
	}

	@Override
	public JavaType<?> getRelationalJavaDescriptor() {
		return relationalJtd;
	}

	@Override
	public JdbcTypeDescriptor getJdbcTypeDescriptor() {
		return jdbcTypeDescriptor;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public JpaAttributeConverter getValueConverter() {
		return valueConverter;
	}

	@Override
	public MutabilityPlan<J> getMutabilityPlan() {
		//noinspection unchecked
		return mutabilityPlan;
	}

	@Override
	public String toString() {
		return "NamedConverterResolution(" + valueConverter.getConverterBean().getBeanClass().getName() + ')';
	}

}
