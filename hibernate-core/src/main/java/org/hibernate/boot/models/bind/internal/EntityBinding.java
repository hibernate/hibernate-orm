/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.annotations.Filter;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.JpaEventListener;
import org.hibernate.boot.models.categorize.spi.JpaEventListenerStyle;
import org.hibernate.boot.models.categorize.spi.MappedSuperclassTypeMetadata;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.event.internal.EntityCallback;
import org.hibernate.jpa.event.internal.ListenerCallback;
import org.hibernate.jpa.event.spi.CallbackDefinition;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;

import jakarta.persistence.Cacheable;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.SharedCacheMode;

/**
 * @author Steve Ebersole
 */
public abstract class EntityBinding extends IdentifiableTypeBinding {

	public EntityBinding(
			EntityTypeMetadata entityTypeMetadata,
			IdentifiableTypeBinding superTypeBinding,
			EntityHierarchy.HierarchyRelation hierarchyRelation,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		super( entityTypeMetadata, superTypeBinding, hierarchyRelation, bindingOptions, bindingState, bindingContext );
	}

	public abstract PersistentClass getPersistentClass();

	@Override
	public EntityTypeMetadata getTypeMetadata() {
		return (EntityTypeMetadata) super.getTypeMetadata();
	}

	protected static void applyNaming(EntityTypeMetadata source, PersistentClass persistentClass, BindingState bindingState) {
		final ClassDetails classDetails = source.getClassDetails();
		final AnnotationUsage<Entity> entityAnn = classDetails.getAnnotationUsage( Entity.class );
		final String jpaEntityName = BindingHelper.getValue( entityAnn, "name", (String) null );
		final String entityName;
		final String importName;

		if ( classDetails.getName() != null
				&& !classDetails.getName().equals( classDetails.getClassName() ) ) {
			// should indicate a dynamic model
			entityName = classDetails.getName();
		}
		else {
			entityName = classDetails.getClassName();
		}

		if ( StringHelper.isNotEmpty( jpaEntityName ) ) {
			importName = jpaEntityName;
		}
		else {
			importName = StringHelper.unqualifyEntityName( entityName );
		}

		persistentClass.setClassName( classDetails.getClassName() );
		persistentClass.setEntityName( entityName );
		persistentClass.setJpaEntityName( importName );

		bindingState.getMetadataBuildingContext().getMetadataCollector().addImport( importName, entityName );
	}

	protected static void applyDiscriminatorValue(
			EntityTypeMetadata typeMetadata,
			PersistentClass persistentClass) {
		final BasicValue discriminatorMapping = (BasicValue) persistentClass.getRootClass().getDiscriminator();
		if ( discriminatorMapping == null ) {
			return;
		}

		final AnnotationUsage<DiscriminatorValue> ann = typeMetadata.getClassDetails().getAnnotationUsage( DiscriminatorValue.class );
		if ( ann == null ) {
			final Type resolvedJavaType = discriminatorMapping.resolve().getRelationalJavaType().getJavaType();
			if ( resolvedJavaType == String.class ) {
				persistentClass.setDiscriminatorValue( persistentClass.getEntityName() );
			}
			else {
				persistentClass.setDiscriminatorValue( Integer.toString( persistentClass.getSubclassId() ) );
			}
		}
		else {
			persistentClass.setDiscriminatorValue( ann.getString( "value" ) );
		}
	}

	protected static void applyCaching(EntityTypeMetadata source, PersistentClass persistentClass, BindingState bindingState) {
		final ClassDetails classDetails = source.getClassDetails();
		final var cacheableAnn = classDetails.getAnnotationUsage( Cacheable.class );
		if ( cacheableAnn == null ) {
			return;
		}

		final SharedCacheMode sharedCacheMode = bindingState.getMetadataBuildingContext()
				.getBuildingOptions()
				.getSharedCacheMode();

		persistentClass.setCached( isCacheable( sharedCacheMode, cacheableAnn ) );
	}

	private static boolean isCacheable(SharedCacheMode sharedCacheMode, AnnotationUsage<Cacheable> explicitCacheableAnn) {
		return switch ( sharedCacheMode ) {
			// all entities should be cached
			case ALL -> true;
			// Hibernate defaults to ENABLE_SELECTIVE, the only sensible setting
			// only entities with @Cacheable(true) should be cached
			case ENABLE_SELECTIVE, UNSPECIFIED -> explicitCacheableAnn != null && explicitCacheableAnn.getBoolean( "value" );
			// only entities with @Cacheable(false) should not be cached
			case DISABLE_SELECTIVE -> explicitCacheableAnn == null || explicitCacheableAnn.getBoolean( "value" );
			// treat both NONE and UNSPECIFIED the same
			default -> false;
		};
	}

	protected static void applyFilters(EntityTypeMetadata source, PersistentClass persistentClass) {
		final ClassDetails classDetails = source.getClassDetails();
		final List<AnnotationUsage<Filter>> filters = classDetails.getRepeatedAnnotationUsages( Filter.class );
		if ( CollectionHelper.isEmpty( filters ) ) {
			return;
		}

		filters.forEach( (filter) -> {
			persistentClass.addFilter(
					filter.getString( "name" ),
					filter.getString( "condition", (String) null ),
					filter.getAttributeValue( "deduceAliasInjectionPoints", true ),
					extractFilterAliasTableMap( filter ),
					extractFilterAliasEntityMap( filter )
			);
		} );
	}

	private static Map<String, String> extractFilterAliasTableMap(AnnotationUsage<Filter> filter) {
		// todo : implement
		return null;
	}

	private static Map<String, String> extractFilterAliasEntityMap(AnnotationUsage<Filter> filter) {
		// todo : implement
		return null;
	}

	protected static void applyJpaEventListeners(EntityTypeMetadata typeMetadata, PersistentClass persistentClass) {
		final List<JpaEventListener> listeners = typeMetadata.getCompleteJpaEventListeners();
		if ( CollectionHelper.isEmpty( listeners ) ) {
			return;
		}

		listeners.forEach( (listener) -> {
			if ( listener.getStyle() == JpaEventListenerStyle.CALLBACK ) {
				processEntityCallbacks( listener, typeMetadata, persistentClass );
			}
			else {
				assert listener.getStyle() == JpaEventListenerStyle.LISTENER;
				processListenerCallbacks( listener, typeMetadata, persistentClass );
			}
		} );
	}

	private static void processEntityCallbacks(
			JpaEventListener listener,
			EntityTypeMetadata typeMetadata,
			PersistentClass persistentClass) {
		final Class<?> entityClass = listener.getCallbackClass().toJavaClass();
		processJpaEventCallbacks(
				entityClass,
				listener,
				JpaEventListenerStyle.CALLBACK,
				null,
				typeMetadata,
				persistentClass
		);
	}

	private static void processJpaEventCallbacks(
			Class<?> listenerClass,
			JpaEventListener listener,
			JpaEventListenerStyle style,
			Class<?> methodArgumentType,
			EntityTypeMetadata typeMetadata,
			PersistentClass persistentClass) {
		assert style == JpaEventListenerStyle.CALLBACK || methodArgumentType != null;

		// todo : would be nicer to allow injecting them one at a time.
		//  		upstream is defined currently to accept a List
		final List<CallbackDefinition> callbackDefinitions = new ArrayList<>();

		final MethodDetails prePersistMethod = listener.getPrePersistMethod();
		if ( prePersistMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, prePersistMethod );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.PRE_PERSIST
			) );
		}

		final MethodDetails postPersistMethod = listener.getPostPersistMethod();
		if ( postPersistMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, postPersistMethod );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.POST_PERSIST
			) );
		}

		final MethodDetails preUpdateMethod = listener.getPreUpdateMethod();
		if ( preUpdateMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, preUpdateMethod );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.PRE_UPDATE
			) );
		}

		final MethodDetails postUpdateMethod = listener.getPostUpdateMethod();
		if ( postUpdateMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, postUpdateMethod );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.POST_UPDATE
			) );
		}

		final MethodDetails preRemoveMethod = listener.getPreRemoveMethod();
		if ( preRemoveMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, preRemoveMethod );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.PRE_REMOVE
			) );
		}

		final MethodDetails postRemoveMethod = listener.getPostRemoveMethod();
		if ( postRemoveMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, postRemoveMethod );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.POST_REMOVE
			) );
		}

		final MethodDetails postLoadMethod = listener.getPostLoadMethod();
		if ( postLoadMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, postLoadMethod );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.POST_LOAD
			) );
		}

		persistentClass.addCallbackDefinitions( callbackDefinitions );
	}

	private static CallbackDefinition createCallbackDefinition(
			Class<?> listenerClass,
			Method callbackMethod,
			JpaEventListenerStyle style,
			CallbackType callbackType) {
		final CallbackDefinition callback;
		if ( style == JpaEventListenerStyle.CALLBACK ) {
			callback = new EntityCallback.Definition( callbackMethod, callbackType );
		}
		else {
			callback = new ListenerCallback.Definition( listenerClass, callbackMethod, callbackType );
		}
		return callback;
	}

	private static void processListenerCallbacks(
			JpaEventListener listener,
			EntityTypeMetadata typeMetadata,
			PersistentClass persistentClass) {
		final Class<?> listenerClass = listener.getCallbackClass().toJavaClass();
		processJpaEventCallbacks(
				listenerClass,
				listener,
				JpaEventListenerStyle.LISTENER,
				typeMetadata.getClassDetails().toJavaClass(),
				typeMetadata,
				persistentClass
		);
	}

	private static Method findCallbackMethod(
			Class<?> callbackTarget,
			MethodDetails callbackMethod) {
		try {
			if ( callbackMethod.getArgumentTypes().isEmpty() ) {
				return callbackTarget.getDeclaredMethod( callbackMethod.getName() );
			}
			else {
				final ClassDetails argClassDetails = callbackMethod.getArgumentTypes().get( 0 );
				// we don't
				return callbackTarget.getMethod( callbackMethod.getName(), argClassDetails.toJavaClass() );
			}
		}
		catch (NoSuchMethodException e) {
			final ModelsException modelsException = new ModelsException(
					String.format(
							Locale.ROOT,
							"Unable to locate callback method - %s.%s",
							callbackTarget.getName(),
							callbackMethod.getName()
					)
			);
			modelsException.addSuppressed( e );
			throw modelsException;
		}
	}

	protected void processSecondaryTables(TableReference primaryTableReference) {
		TableHelper.bindSecondaryTables(
				this,
				primaryTableReference,
				bindingOptions,
				bindingState,
				bindingContext
		);
	}

	protected void prepareSubclassBindings() {
		getTypeMetadata().forEachSubType( (subType) -> {
			if ( subType instanceof EntityTypeMetadata entityTypeMetadata ) {
				new SubclassEntityBinding(
						entityTypeMetadata,
						this,
						EntityHierarchy.HierarchyRelation.SUB,
						bindingOptions,
						bindingState,
						bindingContext
				);
			}
			else {
				new MappedSuperclassBinding(
						(MappedSuperclassTypeMetadata) subType,
						this,
						EntityHierarchy.HierarchyRelation.SUB,
						bindingOptions,
						bindingState,
						bindingContext
				);
			}
		} );
	}

	@Override
	protected boolean excludeAttributeFromPreparation(AttributeMetadata attributeMetadata) {
		// skip "special" attributes
		final EntityHierarchy hierarchy = getTypeMetadata().getHierarchy();
		if ( hierarchy.getIdMapping().contains( attributeMetadata )
				|| hierarchy.getVersionAttribute() == attributeMetadata
				|| hierarchy.getTenantIdAttribute() == attributeMetadata ) {
			return true;
		}

		return super.excludeAttributeFromPreparation( attributeMetadata );
	}

	@Override
	protected AttributeBinding createAttributeBinding(AttributeMetadata attributeMetadata, Table primaryTable) {
		return new AttributeBinding( attributeMetadata, getPersistentClass(), primaryTable, bindingOptions, bindingState, bindingContext );
	}
}
