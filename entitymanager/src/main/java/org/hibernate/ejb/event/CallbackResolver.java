/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.ejb.event;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.ExcludeDefaultListeners;
import javax.persistence.ExcludeSuperclassListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceException;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 */
public final class CallbackResolver {
	private static Logger log = LoggerFactory.getLogger(CallbackResolver.class);	
	
	private static boolean useAnnotationAnnotatedByListener;

	static {
		//check whether reading annotations of annotations is useful or not
		useAnnotationAnnotatedByListener = false;
		Target target = (Target) EntityListeners.class.getAnnotation( Target.class );
		if ( target != null ) {
			for ( ElementType type : target.value() ) {
				if ( type.equals( ElementType.ANNOTATION_TYPE ) ) useAnnotationAnnotatedByListener = true;
			}
		}
	}

	private CallbackResolver() {
	}

	public static Callback[] resolveCallback(XClass beanClass, Class annotation, ReflectionManager reflectionManager) {
		List<Callback> callbacks = new ArrayList<Callback>();
		List<String> callbacksMethodNames = new ArrayList<String>(); //used to track overriden methods
		List<Class> orderedListeners = new ArrayList<Class>();
		XClass currentClazz = beanClass;
		boolean stopListeners = false;
		boolean stopDefaultListeners = false;
		do {
			Callback callback = null;
			List<XMethod> methods = currentClazz.getDeclaredMethods();
			final int size = methods.size();
			for ( int i = 0; i < size ; i++ ) {
				final XMethod xMethod = methods.get( i );
				if ( xMethod.isAnnotationPresent( annotation ) ) {
					Method method = reflectionManager.toMethod( xMethod );
					final String methodName = method.getName();
					if ( ! callbacksMethodNames.contains( methodName ) ) {
						//overriden method, remove the superclass overriden method
						if ( callback == null ) {
							callback = new BeanCallback( method );
							Class returnType = method.getReturnType();
							Class[] args = method.getParameterTypes();
							if ( returnType != Void.TYPE || args.length != 0 ) {
								throw new RuntimeException(
										"Callback methods annotated on the bean class must return void and take no arguments: " + annotation
												.getName() + " - " + xMethod
								);
							}
							if ( ! method.isAccessible() ) {
								method.setAccessible( true );
							}
							log.debug("Adding {} as {} callback for entity {}.", new String[]{methodName, annotation.getSimpleName(), beanClass.getName()});
							callbacks.add( 0, callback ); //superclass first
							callbacksMethodNames.add( 0, methodName );
						}
						else {
							throw new PersistenceException(
									"You can only annotate one callback method with "
											+ annotation.getName() + " in bean class: " + beanClass.getName()
							);
						}
					}
				}
			}
			if ( !stopListeners ) {
				getListeners( currentClazz, orderedListeners );
				stopListeners = currentClazz.isAnnotationPresent( ExcludeSuperclassListeners.class );
				stopDefaultListeners = currentClazz.isAnnotationPresent( ExcludeDefaultListeners.class );
			}

			do {
				currentClazz = currentClazz.getSuperclass();
			}
			while ( currentClazz != null
					&& ! ( currentClazz.isAnnotationPresent( Entity.class )
					|| currentClazz.isAnnotationPresent( MappedSuperclass.class ) )
					);
		}
		while ( currentClazz != null );

		//handle default listeners
		if ( ! stopDefaultListeners ) {
			List<Class> defaultListeners = (List<Class>) reflectionManager.getDefaults().get( EntityListeners.class );

			if ( defaultListeners != null ) {
				int defaultListenerSize = defaultListeners.size();
				for ( int i = defaultListenerSize - 1; i >= 0 ; i-- ) {
					orderedListeners.add( defaultListeners.get( i ) );
				}
			}
		}

		for ( Class listener : orderedListeners ) {
			Callback callback = null;
			if ( listener != null ) {
				XClass xListener = reflectionManager.toXClass( listener );
				callbacksMethodNames = new ArrayList<String>();
				do {
					List<XMethod> methods = xListener.getDeclaredMethods();
					final int size = methods.size();
					for ( int i = 0; i < size ; i++ ) {
						final XMethod xMethod = methods.get( i );
						if ( xMethod.isAnnotationPresent( annotation ) ) {
							final Method method = reflectionManager.toMethod( xMethod );
							final String methodName = method.getName();
							if ( ! callbacksMethodNames.contains( methodName ) ) {
								//overriden method, remove the superclass overriden method
								if ( callback == null ) {
									try {
										callback = new ListenerCallback( method, listener.newInstance() );
									}
									catch (IllegalAccessException e) {
										throw new PersistenceException(
												"Unable to create instance of " + listener.getName()
														+ " as a listener of beanClass", e
										);
									}
									catch (InstantiationException e) {
										throw new PersistenceException(
												"Unable to create instance of " + listener.getName()
														+ " as a listener of beanClass", e
										);
									}
									Class returnType = method.getReturnType();
									Class[] args = method.getParameterTypes();
									if ( returnType != Void.TYPE || args.length != 1 ) {
										throw new PersistenceException(
												"Callback methods annotated in a listener bean class must return void and take one argument: " + annotation
														.getName() + " - " + method
										);
									}
									if ( ! method.isAccessible() ) {
										method.setAccessible( true );
									}
									log.debug("Adding {} as {} callback for entity {}.", new String[]{methodName, annotation.getSimpleName(), beanClass.getName()});
									callbacks.add( 0, callback ); // listeners first
								}
								else {
									throw new PersistenceException(
											"You can only annotate one callback method with "
													+ annotation.getName() + " in bean class: " + beanClass.getName() + " and callback listener: "
													+ listener.getName()
									);
								}
							}
						}
					}
					xListener = null;  //xListener.getSuperclass();
				}
				while ( xListener != null );
			}
		}
		return callbacks.toArray( new Callback[ callbacks.size() ] );
	}

	private static void getListeners(XClass currentClazz, List<Class> orderedListeners) {
		EntityListeners entityListeners = (EntityListeners) currentClazz.getAnnotation( EntityListeners.class );
		if ( entityListeners != null ) {
			Class[] classes = entityListeners.value();
			int size = classes.length;
			for ( int index = size - 1; index >= 0 ; index-- ) {
				orderedListeners.add( classes[index] );
			}
		}
		if ( useAnnotationAnnotatedByListener ) {
			Annotation[] annotations = currentClazz.getAnnotations();
			for ( Annotation annot : annotations ) {
				entityListeners = annot.getClass().getAnnotation( EntityListeners.class );
				if ( entityListeners != null ) {
					Class[] classes = entityListeners.value();
					int size = classes.length;
					for ( int index = size - 1; index >= 0 ; index-- ) {
						orderedListeners.add( classes[index] );
					}
				}
			}
		}
	}
}
