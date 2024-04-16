/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.usertype.UserType;

import jakarta.persistence.AttributeConverter;

import static java.util.Collections.emptyMap;
import static org.hibernate.boot.model.internal.AnnotationHelper.resolveAttributeConverter;
import static org.hibernate.boot.model.internal.AnnotationHelper.resolveBasicType;
import static org.hibernate.boot.model.internal.AnnotationHelper.resolveJavaType;
import static org.hibernate.boot.model.internal.AnnotationHelper.resolveUserType;
import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * @author Gavin King
 */
public class FilterDefBinder {
	private static final CoreMessageLogger LOG = messageLogger( FilterDefBinder.class );

	public static void bindFilterDefs(AnnotationTarget annotatedElement, MetadataBuildingContext context) {
		annotatedElement.forEachAnnotationUsage( FilterDef.class, (usage) -> {
			bindFilterDef( usage, context );
		} );
	}

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

				final ClassDetails resolverClassDetails = explicitParameter.getClassDetails( "resolver" );
				if ( !resolverClassDetails.getName().equals( Supplier.class.getName() ) ) {
					parameterResolvers.put( explicitParameter.getString( "name" ), resolveParamResolver( resolverClassDetails, context ) );
				}
			}
		}

		final FilterDefinition filterDefinition = new FilterDefinition(
				name,
				filterDef.getString( "defaultCondition" ),
				filterDef.getBoolean( "autoEnabled" ),
				filterDef.applyToLoadByKey(),
				paramJdbcMappings,
				parameterResolvers
		);

		LOG.debugf( "Binding filter definition: %s", filterDefinition.getFilterName() );
		context.getMetadataCollector().addFilterDefinition( filterDefinition );
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static ManagedBean<? extends Supplier<?>> resolveParamResolver(ClassDetails resolverClassDetails, MetadataBuildingContext context) {
		final Class<? extends Supplier> clazz = resolverClassDetails.toJavaClass();
		assert clazz != Supplier.class;
		final BootstrapContext bootstrapContext = context.getBootstrapContext();
		return (ManagedBean<? extends Supplier<?>>) bootstrapContext.getServiceRegistry()
						.requireService(ManagedBeanRegistry.class)
						.getBean(clazz, bootstrapContext.getCustomTypeProducer());
	}

	@SuppressWarnings("unchecked")
	public static JdbcMapping resolveFilterParamType(Class<?> type, MetadataBuildingContext context) {
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
}
