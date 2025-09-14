/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.FilterDef;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.usertype.UserType;

import jakarta.persistence.AttributeConverter;

import static java.util.Collections.emptyMap;
import static org.hibernate.boot.model.internal.AnnotationHelper.resolveAttributeConverter;
import static org.hibernate.boot.model.internal.AnnotationHelper.resolveBasicType;
import static org.hibernate.boot.model.internal.AnnotationHelper.resolveJavaType;
import static org.hibernate.boot.model.internal.AnnotationHelper.resolveUserType;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/**
 * @author Gavin King
 */
public class FilterDefBinder {

	public static void bindFilterDefs(AnnotationTarget annotatedElement, MetadataBuildingContext context) {
		final var modelsContext = context.getBootstrapContext().getModelsContext();
		annotatedElement.forEachAnnotationUsage( FilterDef.class, modelsContext, (usage) -> {
			bindFilterDef( usage, context );
		} );
	}

	public static void bindFilterDef(FilterDef filterDef, MetadataBuildingContext context) {
		final String name = filterDef.name();
		if ( context.getMetadataCollector().getFilterDefinition( name ) != null ) {
			throw new AnnotationException( "Multiple '@FilterDef' annotations define a filter named '" + name + "'" );
		}

		final Map<String, JdbcMapping> paramJdbcMappings;
		final Map<String, ManagedBean<? extends Supplier<?>>> parameterResolvers;
		final var explicitParameters = filterDef.parameters();
		if ( isEmpty( explicitParameters ) ) {
			paramJdbcMappings = emptyMap();
			parameterResolvers = emptyMap();
		}
		else {
			paramJdbcMappings = new HashMap<>();
			parameterResolvers = new HashMap<>();
			for ( var explicitParameter : explicitParameters ) {
				final String parameterName = explicitParameter.name();
				final Class<?> typeClassDetails = explicitParameter.type();
				final JdbcMapping jdbcMapping = resolveFilterParamType( typeClassDetails, context );
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

				final var resolverClass = explicitParameter.resolver();
				if ( !Supplier.class.equals( resolverClass ) ) {
					parameterResolvers.put( explicitParameter.name(),
							resolveParamResolver( resolverClass, context ) );
				}
			}
		}

		final var filterDefinition = new FilterDefinition(
				name,
				filterDef.defaultCondition(),
				filterDef.autoEnabled(),
				filterDef.applyToLoadByKey(),
				paramJdbcMappings,
				parameterResolvers
		);

		if ( CORE_LOGGER.isTraceEnabled() ) {
			CORE_LOGGER.trace( "Binding filter definition: " + filterDefinition.getFilterName() );
		}
		context.getMetadataCollector().addFilterDefinition( filterDefinition );
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static ManagedBean<? extends Supplier<?>> resolveParamResolver(Class<? extends Supplier> resolverClass, MetadataBuildingContext context) {
		assert resolverClass != Supplier.class;
		final var bootstrapContext = context.getBootstrapContext();
		return (ManagedBean<? extends Supplier<?>>)
				bootstrapContext.getManagedBeanRegistry()
						.getBean( resolverClass, bootstrapContext.getCustomTypeProducer() );
	}

	@SuppressWarnings("unchecked")
	public static JdbcMapping resolveFilterParamType(Class<?> type, MetadataBuildingContext context) {
		if ( UserType.class.isAssignableFrom( type ) ) {
			return resolveUserType( (Class<UserType<?>>) type, context );
		}
		else if ( AttributeConverter.class.isAssignableFrom( type ) ) {
			return resolveAttributeConverter( (Class<? extends AttributeConverter<?,?>>) type, context );
		}
		else if ( JavaType.class.isAssignableFrom( type ) ) {
			return resolveJavaType( (Class<JavaType<?>>) type, context );
		}
		else {
			return resolveBasicType( type, context );
		}
	}
}
