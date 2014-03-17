/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.internal.annotations.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

/**
 * Helper for working with JPA callbacks/listeners
 *
 * @author Steve Ebersole
 * @author Strong Liu
 */
public class JPAListenerHelper {
	private static final Logger log = Logger.getLogger( JPAListenerHelper.class );

	/**
	 * Find the name of the method in the class (described by the descriptor) that
	 * is annotated with the given lifecycle callback annotation.
	 *
	 * @param javaTypeDescriptor The descriptor for the class in which to find
	 * the lifecycle callback method
	 * @param eventType The type of lifecycle callback to look for
	 * @param listener Is the {@code javaTypeDescriptor} a listener, as opposed to
	 * an Entity/MappedSuperclass?  Used here to validate method signatures.
	 *
	 * @return The name of the callback method, or {@code null} indicating none was found
	 */
	public static String findCallback(JavaTypeDescriptor javaTypeDescriptor, DotName eventType, boolean listener) {
		final Collection<AnnotationInstance> listenerAnnotations = javaTypeDescriptor.getJandexClassInfo()
				.annotations()
				.get( eventType );
		if ( listenerAnnotations == null || listenerAnnotations.isEmpty() ) {
			return null;
		}

		// todo : these log messages on skip really should be warn, which means i18n message logging

		for ( AnnotationInstance listenerAnnotation : listenerAnnotations ) {
			if ( !MethodInfo.class.isInstance( listenerAnnotation.target() ) ) {
				log.debugf(
						"Skipping callback annotation [%s] for class [%s] as it was " +
								"applied to target other than a method : %s",
						eventType,
						javaTypeDescriptor.getName().toString(),
						listenerAnnotation.target()
				);
				continue;
			}

			final MethodInfo methodInfo = (MethodInfo) listenerAnnotation.target();

			// validate return is `void`
			//		NOTE : we do NOT skip here, just a warning
			if ( methodInfo.returnType().kind() != Type.Kind.VOID ) {
				log.debugf(
						"Callback annotation [%s] on class [%s] was applied to method [%s] with a non-void return type",
						eventType,
						javaTypeDescriptor.getName().toString(),
						methodInfo.name()
				);
			}

			// validate arguments
			if ( listener ) {
				// should have a single argument
				if ( methodInfo.args().length != 1 ) {
					log.debugf(
							"Callback annotation [%s] on listener class [%s] was applied to method [%s] " +
									"which does not define a single-arg signature [%s]",
							eventType,
							javaTypeDescriptor.getName().toString(),
							methodInfo.name(),
							methodInfo.args().length
					);
					continue;
				}
			}
			else {
				// should have no arguments
				if ( methodInfo.args().length != 0 ) {
					log.debugf(
							"Callback annotation [%s] on entity class [%s] was applied to method [%s] " +
									"which does not define a no-arg signature [%s]",
							eventType,
							javaTypeDescriptor.getName().toString(),
							methodInfo.name(),
							methodInfo.args().length
					);
					continue;
				}
			}

			// if we did not opt-out above (with a continue statement) then we found it...
			return ( (MethodInfo) listenerAnnotation.target() ).name();
		}

		return null;
	}

	private static final Map<Class<?>, DotName> EVENT_TYPE;

	static {
		EVENT_TYPE = new HashMap<Class<?>, DotName>( 7 );
		EVENT_TYPE.put( PrePersist.class, JPADotNames.PRE_PERSIST );
		EVENT_TYPE.put( PreRemove.class, JPADotNames.PRE_REMOVE );
		EVENT_TYPE.put( PreUpdate.class, JPADotNames.PRE_UPDATE );
		EVENT_TYPE.put( PostLoad.class, JPADotNames.POST_LOAD );
		EVENT_TYPE.put( PostPersist.class, JPADotNames.POST_PERSIST );
		EVENT_TYPE.put( PostRemove.class, JPADotNames.POST_REMOVE );
		EVENT_TYPE.put( PostUpdate.class, JPADotNames.POST_UPDATE );

	}

//	private final EntityTypeMetadata entityTypeMetadata;
//	private final EntityBindingContext context;
//	private final List<ManagedTypeMetadata> mappingClassHierarchy;
//
//	public JPAListenerHelper(EntityTypeMetadata entityTypeMetadata) {
//		this.entityTypeMetadata = entityTypeMetadata;
//		this.context = entityTypeMetadata.getLocalBindingContext();
//		this.mappingClassHierarchy = buildHierarchy();
//	}
//
//	private List<ManagedTypeMetadata> buildHierarchy() {
//		List<ManagedTypeMetadata> list = new ArrayList<ManagedTypeMetadata>();
//		list.add( entityTypeMetadata );
//		if ( !excludeSuperClassListeners( entityTypeMetadata.getClassInfo() ) ) {
//			for ( MappedSuperclassTypeMetadata mappedSuperclassTypeMetadata : entityTypeMetadata.getMappedSuperclassTypeMetadatas() ) {
//				list.add( mappedSuperclassTypeMetadata );
//				if ( excludeSuperClassListeners( mappedSuperclassTypeMetadata.getClassInfo() ) ) {
//					break;
//				}
//
//			}
//		}
//		return list;
//	}
//
//	private List<AnnotationInstance> findAllEntityListeners() {
//		List<AnnotationInstance> result = new ArrayList<AnnotationInstance>();
//		for ( final ManagedTypeMetadata managedTypeMetadata : mappingClassHierarchy ) {
//			List<AnnotationInstance> list = managedTypeMetadata.getClassInfo()
//					.annotations()
//					.get( JPADotNames.ENTITY_LISTENERS );
//			if ( CollectionHelper.isNotEmpty( list ) ) {
//				result.addAll( list );
//			}
//		}
//		return result;
//	}
//
//
//	public List<JpaCallbackSource> bindJPAListeners() {
//		final List<JpaCallbackSource> callbackClassList = new ArrayList<JpaCallbackSource>();
//		bindEntityCallbackEvents( callbackClassList );
//		bindEntityListeners( callbackClassList );
//		bindDefaultListeners( callbackClassList );
//		return callbackClassList;
//	}
//
//	private void bindEntityCallbackEvents(List<JpaCallbackSource> callbackClassList) {
//		Map<String, Void> overrideMethodCheck = new HashMap<String, Void>();
//		for ( final ManagedTypeMetadata managedTypeMetadata : mappingClassHierarchy ) {
//			try {
//				internalProcessCallbacks(
//						managedTypeMetadata.getClassInfo(),
//						callbackClassList,
//						false,
//						false,
//						overrideMethodCheck
//				);
//			}
//			catch ( PersistenceException error ) {
//				throw new PersistenceException( error.getMessage() + "entity listener " + managedTypeMetadata.getName() );
//			}
//		}
//	}
//
//	private void bindEntityListeners(List<JpaCallbackSource> callbackClassList) {
//		List<AnnotationInstance> entityListenerAnnotations = findAllEntityListeners();
//		for ( AnnotationInstance annotation : entityListenerAnnotations ) {
//			Type[] types = annotation.value().asClassArray();
//			for ( int i = types.length - 1; i >= 0; i-- ) {
//				String callbackClassName = types[i].name().toString();
//				try {
//					processJpaCallbacks( callbackClassName, true, callbackClassList, null );
//				}
//				catch ( PersistenceException error ) {
//					throw new PersistenceException( error.getMessage() + "entity listener " + callbackClassName );
//				}
//			}
//		}
//	}
//
//	private void processJpaCallbacks(
//			final String instanceCallbackClassName,
//			final boolean isListener,
//			final List<JpaCallbackSource> callbackClassList,
//			final Map<String, Void> overrideMethodCheck) {
//		final ClassInfo callbackClassInfo = findClassInfoByName( instanceCallbackClassName );
//		internalProcessCallbacks( callbackClassInfo, callbackClassList, isListener, false, overrideMethodCheck );
//	}
//
//
//	private void bindDefaultListeners(final List<JpaCallbackSource> callbackClassList) {
//		// Bind default JPA entity listener callbacks (unless excluded), using superclasses first (unless excluded)
//
//		Collection<AnnotationInstance> defaultEntityListenerAnnotations = context
//				.getIndex()
//				.getAnnotations( PseudoJpaDotNames.DEFAULT_ENTITY_LISTENERS );
//		for ( AnnotationInstance annotation : defaultEntityListenerAnnotations ) {
//			for ( Type callbackClass : annotation.value().asClassArray() ) {
//				String callbackClassName = callbackClass.name().toString();
//				ClassInfo callbackClassInfo = findClassInfoByName( callbackClassName );
//				try {
//					processDefaultJpaCallbacks( callbackClassInfo, callbackClassList );
//				}
//				catch ( PersistenceException error ) {
//					throw new PersistenceException( error.getMessage() + "default entity listener " + callbackClassName );
//				}
//			}
//		}
//	}
//
//	private static boolean excludeDefaultListeners(ClassInfo classInfo) {
//		return classInfo.annotations().containsKey( JPADotNames.EXCLUDE_DEFAULT_LISTENERS );
//	}
//
//	private static boolean excludeSuperClassListeners(ClassInfo classInfo) {
//		return classInfo.annotations().containsKey( JPADotNames.EXCLUDE_SUPERCLASS_LISTENERS );
//	}
//
//	private static boolean isNotRootObject(DotName name) {
//		return name != null && !JandexHelper.OBJECT.equals( name );
//	}
//
//	private void processDefaultJpaCallbacks(
//			final ClassInfo callbackClassInfo,
//			final List<JpaCallbackSource> jpaCallbackClassList) {
//		if ( excludeDefaultListeners( callbackClassInfo ) ) {
//			return;
//		}
//		// Process superclass first if available and not excluded
//		if ( !excludeSuperClassListeners( callbackClassInfo ) ) {
//			DotName superName = callbackClassInfo.superName();
//			if ( isNotRootObject( superName ) ) {
//				processDefaultJpaCallbacks( findClassInfoByName( superName.toString() ), jpaCallbackClassList );
//			}
//		}
//		internalProcessCallbacks( callbackClassInfo, jpaCallbackClassList, true, true, null );
//	}
//
//	private ClassInfo findClassInfoByName(String name) {
//		ClassInfo classInfo = context.getClassInfo( name );
//		if ( classInfo == null ) {
//			JandexHelper.throwNotIndexException( name );
//		}
//		return classInfo;
//	}
//
//	private void internalProcessCallbacks(
//			final ClassInfo callbackClassInfo,
//			final List<JpaCallbackSource> callbackClassList,
//			final boolean isListener,
//			final boolean isDefault,
//			final Map<String, Void> overrideMethodCheck) {
//		final Map<Class<?>, String> callbacksByType = new HashMap<Class<?>, String>( 7 );
//		createCallback(
//				PrePersist.class, callbacksByType, callbackClassInfo, isListener, isDefault, overrideMethodCheck
//		);
//		createCallback(
//				PreRemove.class, callbacksByType, callbackClassInfo, isListener, isDefault, overrideMethodCheck
//		);
//		createCallback(
//				PreUpdate.class, callbacksByType, callbackClassInfo, isListener, isDefault, overrideMethodCheck
//		);
//		createCallback(
//				PostLoad.class, callbacksByType, callbackClassInfo, isListener, isDefault, overrideMethodCheck
//		);
//		createCallback(
//				PostPersist.class, callbacksByType, callbackClassInfo, isListener, isDefault, overrideMethodCheck
//		);
//		createCallback(
//				PostRemove.class, callbacksByType, callbackClassInfo, isListener, isDefault, overrideMethodCheck
//		);
//		createCallback(
//				PostUpdate.class, callbacksByType, callbackClassInfo, isListener, isDefault, overrideMethodCheck
//		);
//		if ( !callbacksByType.isEmpty() ) {
//			final String name = callbackClassInfo.name().toString();
//			final JpaCallbackSource callbackSource = new JpaCallbackSourceImpl( name, callbacksByType, isListener );
//			callbackClassList.add( 0, callbackSource );
//		}
//	}
//
//
//	/**
//	 * @param callbackTypeClass Lifecycle event type class, like {@link javax.persistence.PrePersist},
//	 * {@link javax.persistence.PreRemove}, {@link javax.persistence.PreUpdate}, {@link javax.persistence.PostLoad},
//	 * {@link javax.persistence.PostPersist}, {@link javax.persistence.PostRemove}, {@link javax.persistence.PostUpdate}.
//	 * @param callbacksByClass A map that keyed by the {@param callbackTypeClass} and value is callback method name.
//	 * @param callbackClassInfo Jandex ClassInfo of callback method's container, should be either entity/mapped superclass or entity listener class.
//	 * @param isListener Is this callback method defined in an entity listener class or not.
//	 */
//	private void createCallback(
//			final Class callbackTypeClass,
//			final Map<Class<?>, String> callbacksByClass,
//			final ClassInfo callbackClassInfo,
//			final boolean isListener,
//			final boolean isDefault,
//			final Map<String, Void> overrideMethodCheck) {
//
//		final Collection<AnnotationInstance> annotationInstances;
//		if ( isDefault ) {
//			annotationInstances = context.getIndex().getAnnotations( EVENT_TYPE.get( callbackTypeClass ) );
//		}
//		else {
//			List<AnnotationInstance> temp = callbackClassInfo.annotations().get( EVENT_TYPE.get( callbackTypeClass ) );
//			annotationInstances = temp != null ? temp : Collections.EMPTY_LIST;
//		}
//
//		//there should be only one callback method per callbackType, isn't it?
//		//so only one callbackAnnotation?
//		for ( AnnotationInstance callbackAnnotation : annotationInstances ) {
//			MethodInfo methodInfo = (MethodInfo) callbackAnnotation.target();
//			validateMethod( methodInfo, callbackTypeClass, callbacksByClass, isListener );
//			final String name = methodInfo.name();
//			if ( overrideMethodCheck != null && overrideMethodCheck.containsKey( name ) ) {
//				continue;
//			}
//			else if ( overrideMethodCheck != null ) {
//				overrideMethodCheck.put( name, null );
//			}
//			if ( !isDefault ) {
//				callbacksByClass.put( callbackTypeClass, name );
//			}
//			else if ( methodInfo.declaringClass().name().equals( callbackClassInfo.name() ) ) {
//				if ( methodInfo.args().length != 1 ) {
//					throw new PersistenceException(
//							String.format(
//									"Callback method %s must have exactly one argument defined as either Object or %s in ",
//									name,
//									entityTypeMetadata.getName()
//							)
//					);
//				}
//				callbacksByClass.put( callbackTypeClass, name );
//			}
//		}
//	}
//
//
//	/**
//	 * Applying JPA Spec rules to validate listener callback method mapping.
//	 *
//	 * @param methodInfo The lifecycle callback method.
//	 * @param callbackTypeClass Lifecycle event type class, like {@link javax.persistence.PrePersist},
//	 * {@link javax.persistence.PreRemove}, {@link javax.persistence.PreUpdate}, {@link javax.persistence.PostLoad},
//	 * {@link javax.persistence.PostPersist}, {@link javax.persistence.PostRemove}, {@link javax.persistence.PostUpdate}.
//	 * @param callbacksByClass A map that keyed by the {@param callbackTypeClass} and value is callback method name.
//	 * @param isListener Is this callback method defined in an entity listener class or not.
//	 */
//	private void validateMethod(
//			MethodInfo methodInfo,
//			Class callbackTypeClass,
//			Map<Class<?>, String> callbacksByClass,
//			boolean isListener) {
//		final String name = methodInfo.name();
//
//		if ( methodInfo.returnType().kind() != Type.Kind.VOID ) {
//			throw new PersistenceException( "Callback method " + name + " must have a void return type in " );
//		}
//		if ( Modifier.isStatic( methodInfo.flags() ) || Modifier.isFinal( methodInfo.flags() ) ) {
//			throw new PersistenceException( "Callback method " + name + " must not be static or final in " );
//		}
//		Type[] argTypes = methodInfo.args();
//
//		if ( isListener ) {
//			if ( argTypes.length != 1 ) {
//				throw new PersistenceException( "Callback method " + name + " must have exactly one argument in " );
//			}
//			String argTypeName = argTypes[0].name().toString();
//			if ( !argTypeName.equals( Object.class.getName() ) && !argTypeName.equals( entityTypeMetadata.getName() ) ) {
//				Class typeClass = entityTypeMetadata.getLocalBindingContext().locateClassByName( argTypeName );
//				if ( !typeClass.isAssignableFrom( entityTypeMetadata.getConfiguredClass() ) ) {
//					throw new PersistenceException(
//							"The argument for callback method " + name +
//									" must be defined as either Object or " + entityTypeMetadata.getName() + " in "
//					);
//				}
//			}
//		}
//		else if ( argTypes.length != 0 ) {
//			throw new PersistenceException( "Callback method " + name + " must have no arguments in " );
//		}
//		if ( callbacksByClass.containsKey( callbackTypeClass ) ) {
//			throw new PersistenceException(
//					"Only one method may be annotated as a " + callbackTypeClass.getSimpleName() +
//							" callback method in "
//			);
//		}
//
//	}
}
