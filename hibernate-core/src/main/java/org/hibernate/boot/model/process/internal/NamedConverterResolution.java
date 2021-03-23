/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import javax.persistence.AttributeConverter;

import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.metamodel.mapping.SqlExpressable;
import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.sql.ast.Clause;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.converter.AttributeConverterMutabilityPlanImpl;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptorIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("rawtypes")
public class NamedConverterResolution<J> implements BasicValue.Resolution<J> {

	public static NamedConverterResolution from(
			ConverterDescriptor converterDescriptor,
			Function<TypeConfiguration, BasicJavaDescriptor> explicitJtdAccess,
			Function<TypeConfiguration, SqlTypeDescriptor> explicitStdAccess,
			Function<TypeConfiguration, MutabilityPlan> explicitMutabilityPlanAccess,
			SqlTypeDescriptorIndicators sqlTypeIndicators,
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
			Function<TypeConfiguration, BasicJavaDescriptor> explicitJtdAccess,
			Function<TypeConfiguration, SqlTypeDescriptor> explicitStdAccess,
			Function<TypeConfiguration, MutabilityPlan> explicitMutabilityPlanAccess,
			SqlTypeDescriptorIndicators sqlTypeIndicators,
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
			Function<TypeConfiguration, BasicJavaDescriptor> explicitJtdAccess,
			Function<TypeConfiguration, SqlTypeDescriptor> explicitStdAccess,
			Function<TypeConfiguration, MutabilityPlan> explicitMutabilityPlanAccess,
			JpaAttributeConverter converter, SqlTypeDescriptorIndicators sqlTypeIndicators,
			MetadataBuildingContext context) {
		final TypeConfiguration typeConfiguration = context.getBootstrapContext().getTypeConfiguration();

		final JavaTypeDescriptor explicitJtd = explicitJtdAccess != null
				? explicitJtdAccess.apply( typeConfiguration )
				: null;

		final JavaTypeDescriptor domainJtd = explicitJtd != null
				? explicitJtd
				: converter.getDomainJavaDescriptor();

		final SqlTypeDescriptor explicitStd = explicitStdAccess != null
				? explicitStdAccess.apply( typeConfiguration )
				: null;

		final JavaTypeDescriptor relationalJtd = converter.getRelationalJavaDescriptor();

		final SqlTypeDescriptor relationalStd = explicitStd != null
				? explicitStd
				: relationalJtd.getJdbcRecommendedSqlType( sqlTypeIndicators );

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
				relationalStd,
				converter,
				mutabilityPlan,
				context.getBootstrapContext().getTypeConfiguration()
		);
	}


	private final JavaTypeDescriptor domainJtd;
	private final JavaTypeDescriptor relationalJtd;
	private final SqlTypeDescriptor relationalStd;

	private final JpaAttributeConverter valueConverter;
	private final MutabilityPlan mutabilityPlan;

	private final JdbcMapping jdbcMapping;

	private final BasicType legacyResolvedType;

	@SuppressWarnings("unchecked")
	public NamedConverterResolution(
			JavaTypeDescriptor domainJtd,
			JavaTypeDescriptor relationalJtd,
			SqlTypeDescriptor relationalStd,
			JpaAttributeConverter valueConverter,
			MutabilityPlan mutabilityPlan,
			TypeConfiguration typeConfiguration) {
		assert domainJtd != null;
		this.domainJtd = domainJtd;

		assert relationalJtd != null;
		this.relationalJtd = relationalJtd;

		assert relationalStd != null;
		this.relationalStd = relationalStd;

		assert valueConverter != null;
		this.valueConverter = valueConverter;

		assert mutabilityPlan != null;
		this.mutabilityPlan = mutabilityPlan;

		this.jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve(
				relationalJtd,
				relationalStd
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
				relationalStd,
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
	public JavaTypeDescriptor<J> getDomainJavaDescriptor() {
		//noinspection unchecked
		return domainJtd;
	}

	@Override
	public JavaTypeDescriptor<?> getRelationalJavaDescriptor() {
		return relationalJtd;
	}

	@Override
	public SqlTypeDescriptor getRelationalSqlTypeDescriptor() {
		return relationalStd;
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

	/**
	 * Allows treating the attribute conversion as a jdbc-level reference.
	 * This covers the conversion plus managing the JDBC-value
	 */
	private static class ConverterJdbcMappingImpl implements JdbcMapping, MappingModelExpressable<Object>, SqlExpressable {
		private final JavaTypeDescriptor domainJtd;
		private final JavaTypeDescriptor jdbcJtd;
		private final SqlTypeDescriptor std;
		private final JpaAttributeConverter valueConverter;
		private final MutabilityPlan mutabilityPlan;

		private final ValueExtractor extractor;
		private final ValueBinder binder;
		private final BasicType lowLevelJdbcMapping;

		public ConverterJdbcMappingImpl(
				JavaTypeDescriptor domainJtd,
				JavaTypeDescriptor jdbcJtd,
				SqlTypeDescriptor std,
				JpaAttributeConverter valueConverter,
				MutabilityPlan mutabilityPlan,
				TypeConfiguration typeConfiguration) {
			this.domainJtd = domainJtd;
			this.jdbcJtd = jdbcJtd;
			this.std = std;
			this.valueConverter = valueConverter;
			this.mutabilityPlan = mutabilityPlan;

			this.extractor = std.getExtractor( jdbcJtd );
			this.binder = std.getBinder( jdbcJtd );

			this.lowLevelJdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve( jdbcJtd, std );
		}

		@Override
		public JavaTypeDescriptor getJavaTypeDescriptor() {
			return domainJtd;
		}

		@Override
		public SqlTypeDescriptor getSqlTypeDescriptor() {
			return std;
		}

		@Override
		public ValueExtractor getJdbcValueExtractor() {
			return extractor;
		}

		@Override
		public ValueBinder getJdbcValueBinder() {
			return binder;
		}

		@Override
		public int getJdbcTypeCount() {
			return 1;
		}

		@Override
		public List<JdbcMapping> getJdbcMappings() {
			return Collections.singletonList( this );
		}

		@Override
		public int forEachJdbcType(IndexedConsumer<JdbcMapping> action) {
			action.accept( 0, this );
			return 1;
		}

		@Override
		public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
			action.accept( offset, this );
			return 1;
		}

		@Override
		public Object disassemble(Object value, SharedSessionContractImplementor session) {
			return mutabilityPlan.disassemble( value );
		}

		@Override
		public int forEachJdbcValue(
				Object value,
				Clause clause,
				int offset,
				JdbcValuesConsumer valuesConsumer,
				SharedSessionContractImplementor session) {
			final AttributeConverter converter = (AttributeConverter) valueConverter.getConverterBean().getBeanInstance();
			final Object converted = converter.convertToDatabaseColumn( value );
			valuesConsumer.consume( offset, converted, this );
			return 1;
		}

		@Override
		public int forEachDisassembledJdbcValue(
				Object value,
				Clause clause,
				JdbcValuesConsumer valuesConsumer,
				SharedSessionContractImplementor session) {
			final AttributeConverter converter = (AttributeConverter) valueConverter.getConverterBean().getBeanInstance();
			final Object converted = converter.convertToDatabaseColumn( value );
			valuesConsumer.consume( 0, converted, this );
			return 1;
		}

		@Override
		public int forEachDisassembledJdbcValue(
				Object value,
				Clause clause,
				int offset,
				JdbcValuesConsumer valuesConsumer,
				SharedSessionContractImplementor session) {
			final AttributeConverter converter = (AttributeConverter) valueConverter.getConverterBean().getBeanInstance();
			final Object converted = converter.convertToDatabaseColumn( value );
			valuesConsumer.consume( offset, converted, this );
			return 1;
		}

		@Override
		public int forEachJdbcValue(
				Object value,
				Clause clause,
				JdbcValuesConsumer valuesConsumer,
				SharedSessionContractImplementor session) {
			final AttributeConverter converter = (AttributeConverter) valueConverter.getConverterBean().getBeanInstance();
			final Object converted = converter.convertToDatabaseColumn( value );
			valuesConsumer.consume( 0, converted, this );
			return 1;
		}

		@Override
		public JdbcMapping getJdbcMapping() {
			return lowLevelJdbcMapping;
		}
	}
}
