/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.ParamDef;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.converter.internal.JpaAttributeConverterImpl;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;

import jakarta.persistence.AttributeConverter;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;
import static org.hibernate.boot.model.internal.BinderHelper.getOverridableAnnotation;
import static org.hibernate.internal.CoreLogging.messageLogger;
import static org.hibernate.internal.util.GenericsHelper.extractClass;
import static org.hibernate.internal.util.GenericsHelper.extractParameterizedType;

/**
 * @author Gavin King
 */
class FilterDefBinder {

	private static final CoreMessageLogger LOG = messageLogger( FilterDefBinder.class );

	public static void bindFilterDef(AnnotationUsage<FilterDef> filterDef, MetadataBuildingContext context) {
		final String name = filterDef.getString( "name" );
		if ( context.getMetadataCollector().getFilterDefinition( name ) != null ) {
			throw new AnnotationException( "Multiple '@FilterDef' annotations define a filter named '" + name + "'" );
		}

		final Map<String, JdbcMapping> paramJdbcMappings;
		final Map<String, ManagedBean<? extends Supplier<?>>> parameterResolvers;
		final List<AnnotationUsage<ParamDef>> explicitParameters = filterDef.getList( "parameters" );
		if ( explicitParameters.isEmpty() ) {
			paramJdbcMappings = emptyMap();
			parameterResolvers = emptyMap();
		}
		else {
			paramJdbcMappings = new HashMap<>();
			parameterResolvers = new HashMap<>();
			for ( AnnotationUsage<ParamDef> explicitParameter : explicitParameters ) {
				final String parameterName = explicitParameter.getString( "name" );
				final ClassDetails typeClassDetails = explicitParameter.getClassDetails( "type" );
				final JdbcMapping jdbcMapping = resolveFilterParamType( typeClassDetails.toJavaClass(), context );
				if ( jdbcMapping == null ) {
					throw new MappingException(
							String.format(
									Locale.ROOT,
									"Unable to resolve type specified for parameter (%s) defined for @FilterDef (%s)",
									parameterName,
									name
							)
					);
				}
				paramJdbcMappings.put( parameterName, jdbcMapping );

				if ( paramDef.resolver() != Supplier.class ) {
					parameterResolvers.put( paramDef.name(), resolveParamResolver( paramDef, context ) );
				}
			}
		}

		final FilterDefinition filterDefinition = new FilterDefinition(
				name,
				filterDef.getString( "defaultCondition" ),
				paramJdbcMappings,
				parameterResolvers,
				filterDef.getBoolean( "autoEnabled" ),
				filterDef.applyToLoadByKey()
		);

		LOG.debugf( "Binding filter definition: %s", filterDefinition.getFilterName() );
		context.getMetadataCollector().addFilterDefinition( filterDefinition );
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static ManagedBean<? extends Supplier<?>> resolveParamResolver(ParamDef paramDef, MetadataBuildingContext context) {
		final Class<? extends Supplier> clazz = paramDef.resolver();
		assert clazz != Supplier.class;
		final BootstrapContext bootstrapContext = context.getBootstrapContext();
		return (ManagedBean<? extends Supplier<?>>)
				bootstrapContext.getServiceRegistry()
						.requireService(ManagedBeanRegistry.class)
						.getBean(clazz, bootstrapContext.getCustomTypeProducer());
	}

	@SuppressWarnings("unchecked")
	private static JdbcMapping resolveFilterParamType(Class<?> type, MetadataBuildingContext context) {
		if ( UserType.class.isAssignableFrom( type ) ) {
			return resolveUserType( (Class<UserType<?>>) type, context );
		}
		else if ( AttributeConverter.class.isAssignableFrom( type ) ) {
			return resolveAttributeConverter( (Class<AttributeConverter<?,?>>) type, context );
		}
		else if ( JavaType.class.isAssignableFrom( type ) ) {
			return resolveJavaType( (Class<JavaType<?>>) type, context );
		}
		else {
			return resolveBasicType( type, context );
		}
	}

	private static BasicType<Object> resolveBasicType(Class<?> type, MetadataBuildingContext context) {
		final TypeConfiguration typeConfiguration = context.getBootstrapContext().getTypeConfiguration();
		final JavaType<Object> jtd = typeConfiguration.getJavaTypeRegistry().findDescriptor( type );
		if ( jtd != null ) {
			final JdbcType jdbcType = jtd.getRecommendedJdbcType(
					new JdbcTypeIndicators() {
						@Override
						public TypeConfiguration getTypeConfiguration() {
							return typeConfiguration;
						}

						@Override
						public int getPreferredSqlTypeCodeForBoolean() {
							return context.getPreferredSqlTypeCodeForBoolean();
						}

						@Override
						public int getPreferredSqlTypeCodeForDuration() {
							return context.getPreferredSqlTypeCodeForDuration();
						}

						@Override
						public int getPreferredSqlTypeCodeForUuid() {
							return context.getPreferredSqlTypeCodeForUuid();
						}

						@Override
						public int getPreferredSqlTypeCodeForInstant() {
							return context.getPreferredSqlTypeCodeForInstant();
						}

						@Override
						public int getPreferredSqlTypeCodeForArray() {
							return context.getPreferredSqlTypeCodeForArray();
						}

						@Override
						public Dialect getDialect() {
							return context.getMetadataCollector().getDatabase().getDialect();
						}
					}
			);
			return typeConfiguration.getBasicTypeRegistry().resolve( jtd, jdbcType );
		}
		else {
			return null;
		}
	}

	private static JdbcMapping resolveUserType(Class<UserType<?>> userTypeClass, MetadataBuildingContext context) {
		final BootstrapContext bootstrapContext = context.getBootstrapContext();
		final UserType<?> userType =
				!context.getBuildingOptions().isAllowExtensionsInCdi()
						? FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( userTypeClass )
						: bootstrapContext.getServiceRegistry().requireService( ManagedBeanRegistry.class )
								.getBean( userTypeClass ).getBeanInstance();
		return new CustomType<>( userType, bootstrapContext.getTypeConfiguration() );
	}

	private static JdbcMapping resolveAttributeConverter(Class<AttributeConverter<?, ?>> type, MetadataBuildingContext context) {
		final BootstrapContext bootstrapContext = context.getBootstrapContext();
		final ManagedBean<AttributeConverter<?, ?>> bean =
				bootstrapContext.getServiceRegistry()
						.requireService( ManagedBeanRegistry.class )
						.getBean( type );

		final TypeConfiguration typeConfiguration = bootstrapContext.getTypeConfiguration();
		final JavaTypeRegistry jtdRegistry = typeConfiguration.getJavaTypeRegistry();

		final ParameterizedType converterParameterizedType = extractParameterizedType( bean.getBeanClass() );
		final Class<?> domainJavaClass = extractClass( converterParameterizedType.getActualTypeArguments()[0] );
		final Class<?> relationalJavaClass = extractClass( converterParameterizedType.getActualTypeArguments()[1] );

		final JavaType<?> domainJtd = jtdRegistry.resolveDescriptor( domainJavaClass );
		final JavaType<?> relationalJtd = jtdRegistry.resolveDescriptor( relationalJavaClass );

		final JavaType<? extends AttributeConverter<?,?>> converterJtd =
				jtdRegistry.resolveDescriptor( bean.getBeanClass() );
		@SuppressWarnings({"rawtypes", "unchecked"})
		final JpaAttributeConverterImpl<?,?> valueConverter =
				new JpaAttributeConverterImpl( bean, converterJtd, domainJtd, relationalJtd );
		return new ConvertedBasicTypeImpl<>(
				ConverterDescriptor.TYPE_NAME_PREFIX
						+ valueConverter.getConverterJavaType().getTypeName(),
				String.format(
						"BasicType adapter for AttributeConverter<%s,%s>",
						domainJtd.getTypeName(),
						relationalJtd.getTypeName()
				),
				relationalJtd.getRecommendedJdbcType( typeConfiguration.getCurrentBaseSqlTypeIndicators() ),
				valueConverter
		);
	}

	private static JdbcMapping resolveJavaType(Class<JavaType<?>> type, MetadataBuildingContext context) {
		final TypeConfiguration typeConfiguration = context.getBootstrapContext().getTypeConfiguration();
		final JavaType<?> jtd = getJavaType( type, context, typeConfiguration );
		final JdbcType jdbcType = jtd.getRecommendedJdbcType( typeConfiguration.getCurrentBaseSqlTypeIndicators() );
		return typeConfiguration.getBasicTypeRegistry().resolve( jtd, jdbcType );
	}

	private static JavaType<?> getJavaType(Class<JavaType<?>> javaTypeClass,
			MetadataBuildingContext context,
			TypeConfiguration typeConfiguration) {
		final JavaType<?> registeredJtd = typeConfiguration.getJavaTypeRegistry().findDescriptor( javaTypeClass );
		if ( registeredJtd != null ) {
			return registeredJtd;
		}
		else if ( !context.getBuildingOptions().isAllowExtensionsInCdi() ) {
			return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( javaTypeClass );
		}
		else {
			return context.getBootstrapContext().getServiceRegistry()
					.requireService( ManagedBeanRegistry.class )
					.getBean( javaTypeClass )
					.getBeanInstance();
		}
	}

	public static void bindFilterDefs(AnnotationTarget annotatedElement, MetadataBuildingContext context) {
		final AnnotationUsage<FilterDef> filterDef = annotatedElement.getAnnotationUsage( FilterDef.class );
		final AnnotationUsage<FilterDefs> filterDefs = getOverridableAnnotation( annotatedElement, FilterDefs.class, context );
		if ( filterDef != null ) {
			bindFilterDef( filterDef, context );
		}
		if ( filterDefs != null ) {
			final List<AnnotationUsage<FilterDef>> nestedDefs = filterDefs.getList( "value" );
			for ( AnnotationUsage<FilterDef> def : nestedDefs ) {
				bindFilterDef( def, context );
			}
		}
	}
}
