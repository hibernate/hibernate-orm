/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
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

import org.jboss.logging.Logger;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMethod;
import org.hibernate.ejb.internal.EntityManagerMessageLogger;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.source.binder.JpaCallbackClass;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 */
public final class CallbackResolver {

    private static final EntityManagerMessageLogger LOG = Logger.getMessageLogger(EntityManagerMessageLogger.class,
                                                                           CallbackResolver.class.getName());

	private static boolean useAnnotationAnnotatedByListener;

	static {
		//check whether reading annotations of annotations is useful or not
		useAnnotationAnnotatedByListener = false;
		Target target = EntityListeners.class.getAnnotation( Target.class );
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
		List<String> callbacksMethodNames = new ArrayList<String>(); //used to track overridden methods
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
						//overridden method, remove the superclass overridden method
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
                            if (!method.isAccessible()) method.setAccessible(true);
                            LOG.debugf("Adding %s as %s callback for entity %s",
                                       methodName,
                                       annotation.getSimpleName(),
                                       beanClass.getName());
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
				List<XMethod> methods = xListener.getDeclaredMethods();
				final int size = methods.size();
				for ( int i = 0; i < size ; i++ ) {
					final XMethod xMethod = methods.get( i );
					if ( xMethod.isAnnotationPresent( annotation ) ) {
						final Method method = reflectionManager.toMethod( xMethod );
						final String methodName = method.getName();
						if ( ! callbacksMethodNames.contains( methodName ) ) {
							//overridden method, remove the superclass overridden method
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
                                if (!method.isAccessible()) method.setAccessible(true);
                                LOG.debugf("Adding %s as %s callback for entity %s",
                                           methodName,
                                           annotation.getSimpleName(),
                                           beanClass.getName());
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
			}
		}
		return callbacks.toArray( new Callback[ callbacks.size() ] );
	}

    public static Callback[] resolveCallbacks( Class<?> entityClass,
                                               Class<?> callbackClass,
                                               ClassLoaderService classLoaderService,
                                               EntityBinding binding ) {
        List<Callback> callbacks = new ArrayList<Callback>();
        for (JpaCallbackClass jpaCallbackClass : binding.getJpaCallbackClasses()) {
            Object listener = classLoaderService.classForName(jpaCallbackClass.getName());
            String methodName = jpaCallbackClass.getCallbackMethod( callbackClass );
            Callback callback = jpaCallbackClass.isListener() ?
                                createListenerCallback(entityClass, callbackClass, listener, methodName) :
                                createBeanCallback(callbackClass, methodName);
            LOG.debugf("Adding %s as %s callback for entity %s", methodName, callbackClass.getName(),
                       entityClass.getName());
            assert callback != null;
            callbacks.add(callback);
        }
        return callbacks.toArray(new Callback[callbacks.size()]);
    }

    private static Callback createListenerCallback( Class<?> entityClass,
                                                    Class<?> callbackClass,
                                                    Object listener,
                                                    String methodName ) {
        Class<?> callbackSuperclass = callbackClass.getSuperclass();
        if (callbackSuperclass != null) {
            Callback callback = createListenerCallback(entityClass, callbackSuperclass, listener, methodName);
            if (callback != null) return callback;
        }
        for (Method method : callbackClass.getDeclaredMethods()) {
            if (!method.getName().equals(methodName)) continue;
            Class<?>[] argTypes = method.getParameterTypes();
            if (argTypes.length != 1) continue;
            Class<?> argType = argTypes[0];
            if (argType != Object.class && argType != entityClass) continue;
            if (!method.isAccessible()) method.setAccessible(true);
            return new ListenerCallback(method, listener);
        }
        return null;
    }

    private static Callback createBeanCallback( Class<?> callbackClass,
                                                String methodName ) {
        Class<?> callbackSuperclass = callbackClass.getSuperclass();
        if (callbackSuperclass != null) {
            Callback callback = createBeanCallback(callbackSuperclass, methodName);
            if (callback != null) return callback;
        }
        for (Method method : callbackClass.getDeclaredMethods()) {
            if (!method.getName().equals(methodName)) continue;
            if (method.getParameterTypes().length != 0) continue;
            if (!method.isAccessible()) method.setAccessible(true);
            return new BeanCallback(method);
        }
        return null;
    }

	private static void getListeners(XClass currentClazz, List<Class> orderedListeners) {
		EntityListeners entityListeners = currentClazz.getAnnotation( EntityListeners.class );
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
