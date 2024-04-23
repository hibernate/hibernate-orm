/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.bind.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.Synchronize;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.JpaEventListener;
import org.hibernate.boot.models.categorize.spi.JpaEventListenerStyle;
import org.hibernate.boot.models.categorize.spi.MappedSuperclassTypeMetadata;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
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

import static org.hibernate.boot.models.bind.ModelBindingLogging.MODEL_BINDING_MSG_LOGGER;

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

	protected static void applyCommonInformation(EntityTypeMetadata typeMetadata, PersistentClass persistentClass, BindingState bindingState) {
		applyCaching( typeMetadata, persistentClass, bindingState );
		applyFilters( typeMetadata, persistentClass );
		applyJpaEventListeners( typeMetadata, persistentClass );
		applyBatchSize( typeMetadata, persistentClass, bindingState );
		applySqlCustomizations( typeMetadata, persistentClass, bindingState );
		applySynchronizedTableNames( typeMetadata, persistentClass, bindingState );
	}

	/**
	 * @apiNote Not part of {@linkplain #applyCommonInformation} to allow the difference that we
	 * do not always want this for the root entity
	 */
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
					filter.getString( "condition" ),
					filter.getAttributeValue( "deduceAliasInjectionPoints" ),
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

	private static void applyBatchSize(
			EntityTypeMetadata typeMetadata,
			PersistentClass persistentClass,
			BindingState bindingState) {
		final AnnotationUsage<BatchSize> batchSizeAnnotation = typeMetadata
				.getClassDetails()
				.getAnnotationUsage( HibernateAnnotations.BATCH_SIZE );
		if ( batchSizeAnnotation == null ) {
			return;
		}

		persistentClass.setBatchSize( batchSizeAnnotation.getInteger( "size" ) );
	}

	private static void applySqlCustomizations(
			EntityTypeMetadata typeMetadata,
			PersistentClass persistentClass,
			BindingState bindingState) {
		final AnnotationUsage<DynamicInsert> dynamicInsert = typeMetadata
				.getClassDetails()
				.getAnnotationUsage( HibernateAnnotations.DYNAMIC_INSERT );
		final AnnotationUsage<DynamicUpdate> dynamicUpdate = typeMetadata
				.getClassDetails()
				.getAnnotationUsage( HibernateAnnotations.DYNAMIC_UPDATE );

		final List<AnnotationUsage<SQLInsert>> customInserts = typeMetadata
				.getClassDetails()
				.getRepeatedAnnotationUsages( HibernateAnnotations.SQL_INSERT );
		final List<AnnotationUsage<SQLUpdate>> customUpdates = typeMetadata
				.getClassDetails()
				.getRepeatedAnnotationUsages( HibernateAnnotations.SQL_UPDATE );
		final List<AnnotationUsage<SQLDelete>> customDeletes = typeMetadata
				.getClassDetails()
				.getRepeatedAnnotationUsages( HibernateAnnotations.SQL_DELETE );

		if ( dynamicInsert != null ) {
			if ( CollectionHelper.isNotEmpty( customInserts ) ) {
				MODEL_BINDING_MSG_LOGGER.dynamicAndCustomInsert( persistentClass.getEntityName() );
			}
			persistentClass.setDynamicInsert( dynamicInsert.getBoolean( "value" ) );
		}

		if ( dynamicUpdate != null ) {
			if ( CollectionHelper.isNotEmpty( customUpdates ) ) {
				MODEL_BINDING_MSG_LOGGER.dynamicAndCustomUpdate( persistentClass.getEntityName() );
			}
			persistentClass.setDynamicUpdate( dynamicUpdate.getBoolean( "value" ) );
		}

		if ( CollectionHelper.isNotEmpty( customInserts )
				|| CollectionHelper.isNotEmpty( customUpdates )
				|| CollectionHelper.isNotEmpty( customDeletes ) ) {
			final Map<String,Join> joinMap = extractJoinMap( persistentClass );
			applyCustomSql(
					customInserts,
					persistentClass,
					joinMap,
					PersistentClass::setCustomSQLInsert,
					Join::setCustomSQLInsert
			);
			applyCustomSql(
					customUpdates,
					persistentClass,
					joinMap,
					PersistentClass::setCustomSQLUpdate,
					Join::setCustomSQLUpdate
			);
			applyCustomSql(
					customDeletes,
					persistentClass,
					joinMap,
					PersistentClass::setCustomSQLDelete,
					Join::setCustomSQLDelete
			);
		}
	}

	private static Map<String, Join> extractJoinMap(PersistentClass persistentClass) {
		final List<Join> joins = persistentClass.getJoins();
		if ( CollectionHelper.isEmpty( joins ) ) {
			return Collections.emptyMap();
		}

		final HashMap<String, Join> joinMap = CollectionHelper.mapOfSize( joins.size() );
		joins.forEach( (join) -> joinMap.put( join.getTable().getName(), join ) );
		return joinMap;
	}

	private static <A extends Annotation> void applyCustomSql(
			List<AnnotationUsage<A>> annotationUsages,
			PersistentClass persistentClass,
			Map<String,Join> joinMap,
			PrimaryCustomSqlInjector primaryTableInjector,
			SecondaryCustomSqlInjector secondaryTableInjector) {
		if ( CollectionHelper.isEmpty( annotationUsages ) ) {
			return;
		}

		annotationUsages.forEach( annotationUsage -> {
			final String tableName = annotationUsage.getString( "table" );

			if ( StringHelper.isEmpty( tableName ) ) {
				primaryTableInjector.injectCustomSql(
						persistentClass,
						annotationUsage.getString( "sql" ),
						annotationUsage.getBoolean( "callable" ),
						ExecuteUpdateResultCheckStyle.fromResultCheckStyle( annotationUsage.getEnum( "", ResultCheckStyle.class ) )
				);
			}
			else {
				final Join join = joinMap.get( tableName );
				secondaryTableInjector.injectCustomSql(
						join,
						annotationUsage.getString( "sql" ),
						annotationUsage.getBoolean( "callable" ),
						ExecuteUpdateResultCheckStyle.fromResultCheckStyle( annotationUsage.getEnum( "", ResultCheckStyle.class ) )
				);
			}
		} );
	}

	public abstract RootEntityBinding getRootEntityBinding();

	@FunctionalInterface
	private interface PrimaryCustomSqlInjector {
		void injectCustomSql(PersistentClass persistentClass, String sql, boolean callable, ExecuteUpdateResultCheckStyle checkStyle);
	}

	@FunctionalInterface
	private interface SecondaryCustomSqlInjector {
		void injectCustomSql(Join join, String sql, boolean callable, ExecuteUpdateResultCheckStyle checkStyle);
	}

	private static void applySynchronizedTableNames(
			EntityTypeMetadata typeMetadata,
			PersistentClass persistentClass,
			BindingState bindingState) {
		final AnnotationUsage<Synchronize> usage = typeMetadata
				.getClassDetails()
				.getAnnotationUsage( HibernateAnnotations.SYNCHRONIZE );
		if ( usage == null ) {
			return;
		}

		// todo : handle Synchronize#logical - for now assume it is logical
		final List<String> names = usage.getList( "value" );
		names.forEach( persistentClass::addSynchronizedTable );
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
