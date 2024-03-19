/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.event.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.models.categorize.spi.GlobalRegistrations;
import org.hibernate.boot.models.categorize.spi.JpaEventListener;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.event.spi.CallbackDefinition;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.mapping.Property;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.property.access.spi.Getter;

import org.jboss.logging.Logger;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.ExcludeDefaultListeners;
import jakarta.persistence.ExcludeSuperclassListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PersistenceException;

/**
 * Resolves JPA callback definitions
 *
 * @author Steve Ebersole
 */
public final class CallbackDefinitionResolver {
	private static final Logger log = Logger.getLogger( CallbackDefinitionResolver.class );

	public static List<CallbackDefinition> resolveEntityCallbacks(
			MetadataBuildingContext metadataBuildingContext,
			ClassDetails entityClass,
			CallbackType callbackType) {
		final GlobalRegistrations globalRegistrations = metadataBuildingContext.getMetadataCollector().getGlobalRegistrations();
		final List<JpaEventListener> globalListenerRegistrations = globalRegistrations.getEntityListenerRegistrations();

		List<CallbackDefinition> callbackDefinitions = new ArrayList<>();
		List<String> callbacksMethodNames = new ArrayList<>();
		List<ClassDetails> orderedListeners = new ArrayList<>();

		ClassDetails currentClazz = entityClass;
		boolean stopListeners = false;
		boolean stopDefaultListeners = false;

		do {
			CallbackDefinition callbackDefinition = null;
			final List<MethodDetails> methodsDetailsList = currentClazz.getMethods();
			for ( MethodDetails methodDetails : methodsDetailsList ) {
				if ( !methodDetails.hasAnnotationUsage( callbackType.getCallbackAnnotation() ) ) {
					continue;
				}
				if ( callbacksMethodNames.contains( methodDetails.getName() ) ) {
					continue;
				}

				//overridden method, remove the superclass overridden method
				if ( callbackDefinition == null ) {
					final Method javaMethod = (Method) methodDetails.toJavaMember();
					callbackDefinition = new EntityCallback.Definition( javaMethod, callbackType );
					Class<?> returnType = javaMethod.getReturnType();
					Class<?>[] args = javaMethod.getParameterTypes();
					if ( returnType != Void.TYPE || args.length != 0 ) {
						throw new RuntimeException(
								"Callback methods annotated on the bean class must return void and take no arguments: "
										+ callbackType.getCallbackAnnotation().getName() + " - " + methodDetails
						);
					}
					ReflectHelper.ensureAccessibility( javaMethod );
					if ( log.isDebugEnabled() ) {
						log.debugf(
								"Adding %s as %s callback for entity %s",
								methodDetails.getName(),
								callbackType.getCallbackAnnotation().getSimpleName(),
								entityClass.getName()
						);
					}
					callbackDefinitions.add( 0, callbackDefinition ); //superclass first
					callbacksMethodNames.add( 0, methodDetails.getName() );
				}
				else {
					throw new PersistenceException(
							"You can only annotate one callback method with "
									+ callbackType.getCallbackAnnotation().getName() + " in bean class: " + entityClass.getName()
					);
				}
			}
			if ( !stopListeners ) {
				applyListeners( currentClazz, orderedListeners );
				stopListeners = currentClazz.hasAnnotationUsage( ExcludeSuperclassListeners.class );
				stopDefaultListeners = currentClazz.hasAnnotationUsage( ExcludeDefaultListeners.class );
			}

			do {
				currentClazz = currentClazz.getSuperClass();
			}
			while ( currentClazz != null
					&& !( currentClazz.hasAnnotationUsage( Entity.class )
					|| currentClazz.hasAnnotationUsage( MappedSuperclass.class ) )
					);
		}
		while ( currentClazz != null );

		//handle default listeners
		if ( !stopDefaultListeners ) {
			if ( CollectionHelper.isNotEmpty( globalListenerRegistrations ) ) {
				int defaultListenerSize = globalListenerRegistrations.size();
				for ( int i = defaultListenerSize - 1; i >= 0; i-- ) {
					orderedListeners.add( globalListenerRegistrations.get( i ).getCallbackClass() );
				}
			}
		}

		for ( ClassDetails listenerClassDetails : orderedListeners ) {
			CallbackDefinition callbackDefinition = null;
			if ( listenerClassDetails != null ) {
				for ( MethodDetails methodDetails : listenerClassDetails.getMethods() ) {
					if ( methodDetails.hasAnnotationUsage( callbackType.getCallbackAnnotation() ) ) {
						final String methodName = methodDetails.getName();
						//overridden method, remove the superclass overridden method
						if ( callbackDefinition == null ) {
							final Method method = (Method) methodDetails.toJavaMember();
							callbackDefinition = new ListenerCallback.Definition(
									listenerClassDetails.toJavaClass(),
									method,
									callbackType
							);

							final Class<?> returnType = method.getReturnType();
							final Class<?>[] args = method.getParameterTypes();
							if ( returnType != Void.TYPE || args.length != 1 ) {
								throw new PersistenceException(
										"Callback methods annotated in a listener bean class must return void and take one argument: "
												+ callbackType.getCallbackAnnotation().getName() + " - " + methodDetails
								);
							}
							ReflectHelper.ensureAccessibility( method );
							if ( log.isDebugEnabled() ) {
								log.debugf(
										"Adding %s as %s callback for entity %s",
										methodName,
										callbackType.getCallbackAnnotation().getSimpleName(),
										entityClass.getName()
									);
							}
							callbackDefinitions.add( 0, callbackDefinition ); // listeners first
						}
						else {
							throw new PersistenceException(
									"You can only annotate one callback method with "
											+ callbackType.getCallbackAnnotation().getName()
											+ " in bean class: " + entityClass.getName()
											+ " and callback listener: " + listenerClassDetails.getName()
							);
						}
					}
				}
			}
		}
		return callbackDefinitions;
	}

	public static List<CallbackDefinition> resolveEmbeddableCallbacks(
			MetadataBuildingContext metadataBuildingContext,
			Class<?> entityClass,
			Property embeddableProperty,
			CallbackType callbackType) {
		final SourceModelBuildingContext hibernateModelsContext = metadataBuildingContext.getMetadataCollector().getSourceModelBuildingContext();
		final ClassDetailsRegistry classDetailsRegistry = hibernateModelsContext.getClassDetailsRegistry();

		final Class<?> embeddableClass = embeddableProperty.getType().getReturnedClass();
		final ClassDetails embeddableClassDetails = classDetailsRegistry.getClassDetails( embeddableClass.getName() );

		final Getter embeddableGetter = embeddableProperty.getGetter( entityClass );
		final List<CallbackDefinition> callbackDefinitions = new ArrayList<>();
		final List<String> callbacksMethodNames = new ArrayList<>();
		ClassDetails currentClazz = embeddableClassDetails;
		do {
			CallbackDefinition callbackDefinition = null;
			final List<MethodDetails> methodsDetailsList = currentClazz.getMethods();
			for ( MethodDetails methodDetails : methodsDetailsList ) {
				if ( !methodDetails.hasAnnotationUsage( callbackType.getCallbackAnnotation() ) ) {
					continue;
				}

				final Method method = (Method) methodDetails.toJavaMember();
				final String methodName = method.getName();

				if ( callbacksMethodNames.contains( methodName ) ) {
					throw new PersistenceException(
							"You can only annotate one callback method with "
									+ callbackType.getCallbackAnnotation().getName() + " in bean class: " + currentClazz.getName()
					);
				}

				//overridden method, remove the superclass overridden method
				if ( callbackDefinition == null ) {
					callbackDefinition = new EmbeddableCallback.Definition( embeddableGetter, method, callbackType );
					Class<?> returnType = method.getReturnType();
					Class<?>[] args = method.getParameterTypes();
					if ( returnType != Void.TYPE || args.length != 0 ) {
						throw new RuntimeException(
								"Callback methods annotated on the bean class must return void and take no arguments: "
										+ callbackType.getCallbackAnnotation().getName() + " - " + methodDetails
						);
					}
					ReflectHelper.ensureAccessibility( method );
					if ( log.isDebugEnabled() ) {
						log.debugf(
								"Adding %s as %s callback for entity %s",
								methodName,
								callbackType.getCallbackAnnotation().getSimpleName(),
								currentClazz.getName()
						);
					}
					callbackDefinitions.add( 0, callbackDefinition ); //superclass first
					callbacksMethodNames.add( 0, methodName );
				}
			}

			do {
				currentClazz = currentClazz.getSuperClass();
			}
			while ( currentClazz != null && !currentClazz.hasAnnotationUsage( MappedSuperclass.class ) );
		}
		while ( currentClazz != null );

		return callbackDefinitions;
	}

	private static boolean useAnnotationAnnotatedByListener;

	static {
		//check whether reading annotations of annotations is useful or not
		useAnnotationAnnotatedByListener = false;
		Target target = EntityListeners.class.getAnnotation( Target.class );
		if ( target != null ) {
			for ( ElementType type : target.value() ) {
				if ( type.equals( ElementType.ANNOTATION_TYPE ) ) {
					useAnnotationAnnotatedByListener = true;
					break;
				}
			}
		}
	}

	private static void applyListeners(ClassDetails currentClazz, List<ClassDetails> listOfListeners) {
		final AnnotationUsage<EntityListeners> entityListeners = currentClazz.getAnnotationUsage( EntityListeners.class );
		if ( entityListeners != null ) {
			final List<ClassDetails> listeners = entityListeners.getList( "value" );
			int size = listeners.size();
			for ( int index = size - 1; index >= 0; index-- ) {
				listOfListeners.add( listeners.get( index ) );
			}
		}

		if ( useAnnotationAnnotatedByListener ) {
			final List<AnnotationUsage<?>> metaAnnotatedUsageList = currentClazz.getMetaAnnotated( EntityListeners.class );
			for ( AnnotationUsage<?> metaAnnotatedUsage : metaAnnotatedUsageList ) {
				final AnnotationUsage<EntityListeners> metaAnnotatedListeners = metaAnnotatedUsage.getAnnotationDescriptor().getAnnotationUsage( EntityListeners.class );
				final List<ClassDetails> listeners = metaAnnotatedListeners.getList( "value" );
				int size = listeners.size();
				for ( int index = size - 1; index >= 0; index-- ) {
					listOfListeners.add( listeners.get( index ) );
				}
			}
		}
	}
}
