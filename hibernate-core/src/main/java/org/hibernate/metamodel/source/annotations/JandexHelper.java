/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.annotations;

import java.beans.Introspector;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * Utility methods for working with the jandex annotation index.
 *
 * @author Hardy Ferentschik
 */
public class JandexHelper {
	private static final Map<String, Object> DEFAULT_VALUES_BY_ELEMENT = new HashMap<String, Object>();

	private JandexHelper() {
	}

	/**
	 * Retrieves a jandex annotation element value. If the value is {@code null}, the default value specified in the
	 * annotation class is retrieved instead.
	 * <p>
	 * There are two special cases. {@code Class} parameters should be retrieved as strings (and then can later be
	 * loaded) and enumerated values should be retrieved via {@link #getEnumValue(AnnotationInstance, String, Class)}.
	 * </p>
	 *
	 * @param annotation the annotation containing the element with the supplied name
	 * @param element the name of the element value to be retrieve
	 * @param type the type of element to retrieve. The following types are supported:
	 * <ul>
	 * <li>Byte</li>
	 * <li>Short</li>
	 * <li>Integer</li>
	 * <li>Character</li>
	 * <li>Float</li>
	 * <li>Double</li>
	 * <li>Long</li>
	 * <li>Boolean</li>
	 * <li>String</li>
	 * <li>AnnotationInstance</li>
	 *
	 * @return the value if not {@code null}, else the default value if not
	 *         {@code null}, else {@code null}.
	 *
	 * @throws AssertionFailure in case the specified {@code type} is a class instance or the specified type causes a {@code ClassCastException}
	 * when retrieving the value.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getValue(AnnotationInstance annotation, String element, Class<T> type) throws AssertionFailure {
		if ( Class.class.equals( type ) ) {
			throw new AssertionFailure(
					"Annotation parameters of type Class should be retrieved as strings (fully qualified class names)"
			);
		}

		// try getting the untyped value from Jandex
		AnnotationValue annotationValue = annotation.value( element );

		try {
			if ( annotationValue != null ) {
				return explicitAnnotationParameter( annotationValue, type );
			}
			else {
				return defaultAnnotationParameter( getDefaultValue( annotation, element ), type );
			}
		}
		catch ( ClassCastException e ) {
			throw new AssertionFailure(
					String.format(
							"the annotation property %s of annotation %s is not of type %s",
							element,
							annotation.name(),
							type.getName()
					)
			);
		}
	}

	/**
	 * Retrieves a jandex annotation element value, converting it to the supplied enumerated type.  If the value is
	 * <code>null</code>, the default value specified in the annotation class is retrieved instead.
	 *
	 * @param <T> an enumerated type
	 * @param annotation the annotation containing the enumerated element with the supplied name
	 * @param element the name of the enumerated element value to be retrieve
	 * @param type the type to which to convert the value before being returned
	 *
	 * @return the value converted to the supplied enumerated type if the value is not <code>null</code>, else the default value if
	 *         not <code>null</code>, else <code>null</code>.
	 *
	 * @see #getValue(AnnotationInstance, String, Class)
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Enum<T>> T getEnumValue(AnnotationInstance annotation, String element, Class<T> type) {
		AnnotationValue val = annotation.value( element );
		if ( val == null ) {
			return (T) getDefaultValue( annotation, element );
		}
		return Enum.valueOf( type, val.asEnum() );
	}

	/**
	 * Expects a method or field annotation target and returns the property name for this target
	 *
	 * @param target the annotation target
	 *
	 * @return the property name of the target. For a field it is the field name and for a method name it is
	 *         the method name stripped of 'is', 'has' or 'get'
	 */
	public static String getPropertyName(AnnotationTarget target) {
		if ( !( target instanceof MethodInfo || target instanceof FieldInfo ) ) {
			throw new AssertionFailure( "Unexpected annotation target " + target.toString() );
		}

		if ( target instanceof FieldInfo ) {
			return ( (FieldInfo) target ).name();
		}
		else {
			final String methodName = ( (MethodInfo) target ).name();
			String propertyName;
			if ( methodName.startsWith( "is" ) ) {
				propertyName = Introspector.decapitalize( methodName.substring( 2 ) );
			}
			else if ( methodName.startsWith( "has" ) ) {
				propertyName = Introspector.decapitalize( methodName.substring( 3 ) );
			}
			else if ( methodName.startsWith( "get" ) ) {
				propertyName = Introspector.decapitalize( methodName.substring( 3 ) );
			}
			else {
				throw new AssertionFailure( "Expected a method following the Java Bean notation" );
			}
			return propertyName;
		}
	}

	/**
	 * @param classInfo the class info from which to retrieve the annotation instance
	 * @param annotationName the annotation to retrieve from the class info
	 *
	 * @return the single annotation defined on the class or {@code null} in case the annotation is not specified at all
	 *
	 * @throws org.hibernate.AssertionFailure in case there is there is more than one annotation of this type.
	 */
	public static AnnotationInstance getSingleAnnotation(ClassInfo classInfo, DotName annotationName)
			throws AssertionFailure {
		return getSingleAnnotation( classInfo.annotations(), annotationName );
	}

	/**
	 * @param annotations List of annotation instances keyed against their dot name.
	 * @param annotationName the annotation to retrieve from map
	 *
	 * @return the single annotation of the specified dot name or {@code null} in case the annotation is not specified at all
	 *
	 * @throws org.hibernate.AssertionFailure in case there is there is more than one annotation of this type.
	 */
	public static AnnotationInstance getSingleAnnotation(Map<DotName, List<AnnotationInstance>> annotations, DotName annotationName)
			throws AssertionFailure {
		List<AnnotationInstance> annotationList = annotations.get( annotationName );
		if ( annotationList == null ) {
			return null;
		}
		else if ( annotationList.size() == 1 ) {
			return annotationList.get( 0 );
		}
		else {
			throw new AssertionFailure(
					"Found more than one instance of the annotation "
							+ annotationList.get( 0 ).name().toString()
							+ ". Expected was one."
			);
		}
	}

	/**
	 * @param annotations List of annotation instances keyed against their dot name.
	 * @param annotationName the annotation to check
	 *
	 * @return returns {@code true} if the map contains only a single instance of specified annotation or {@code false} otherwise.
	 *
	 * @throws org.hibernate.AssertionFailure in case there is there is more than one annotation of this type.
	 */
	public static boolean containsSingleAnnotations(Map<DotName, List<AnnotationInstance>> annotations, DotName annotationName)
			throws AssertionFailure {
		return getSingleAnnotation( annotations, annotationName ) != null;
	}

	/**
	 * Creates a jandex index for the specified classes
	 *
	 * @param classLoaderService class loader service
	 * @param classes the classes to index
	 *
	 * @return an annotation repository w/ all the annotation discovered in the specified classes
	 */
	public static Index indexForClass(ClassLoaderService classLoaderService, Class<?>... classes) {
		Indexer indexer = new Indexer();
		for ( Class<?> clazz : classes ) {
			InputStream stream = classLoaderService.locateResourceStream(
					clazz.getName().replace( '.', '/' ) + ".class"
			);
			try {
				indexer.index( stream );
			}
			catch ( IOException e ) {
				StringBuilder builder = new StringBuilder();
				builder.append( "[" );
				int count = 0;
				for ( Class<?> c : classes ) {
					builder.append( c.getName() );
					if ( count < classes.length - 1 ) {
						builder.append( "," );
					}
					count++;
				}
				builder.append( "]" );
				throw new HibernateException( "Unable to create annotation index for " + builder.toString() );
			}
		}
		return indexer.complete();
	}

	public static Map<DotName, List<AnnotationInstance>> getMemberAnnotations(ClassInfo classInfo, String name) {
		if ( classInfo == null ) {
			throw new IllegalArgumentException( "classInfo cannot be null" );
		}

		if ( name == null ) {
			throw new IllegalArgumentException( "name cannot be null" );
		}

		Map<DotName, List<AnnotationInstance>> annotations = new HashMap<DotName, List<AnnotationInstance>>();
		for ( List<AnnotationInstance> annotationList : classInfo.annotations().values() ) {
			for ( AnnotationInstance instance : annotationList ) {
				String targetName = null;
				if ( instance.target() instanceof FieldInfo ) {
					targetName = ( (FieldInfo) instance.target() ).name();
				}
				else if ( instance.target() instanceof MethodInfo ) {
					targetName = ( (MethodInfo) instance.target() ).name();
				}
				if ( targetName != null && name.equals( targetName ) ) {
					addAnnotationToMap( instance, annotations );
				}
			}
		}
		return annotations;
	}

	private static void addAnnotationToMap(AnnotationInstance instance, Map<DotName, List<AnnotationInstance>> annotations) {
		DotName dotName = instance.name();
		List<AnnotationInstance> list;
		if ( annotations.containsKey( dotName ) ) {
			list = annotations.get( dotName );
		}
		else {
			list = new ArrayList<AnnotationInstance>();
			annotations.put( dotName, list );
		}
		list.add( instance );
	}

	private static Object getDefaultValue(AnnotationInstance annotation, String element) {
		String name = annotation.name().toString();
		String fqElement = name + '.' + element;
		Object val = DEFAULT_VALUES_BY_ELEMENT.get( fqElement );
		if ( val != null ) {
			return val;
		}
		try {
			val = Index.class.getClassLoader().loadClass( name ).getMethod( element ).getDefaultValue();
			DEFAULT_VALUES_BY_ELEMENT.put( fqElement, val );
			return val == null ? null : val;
		}
		catch ( RuntimeException error ) {
			throw error;
		}
		catch ( Exception error ) {
			throw new AssertionFailure(
					String.format( "The annotation %s does not define a parameter '%s'", name, element ),
					error
			);
		}
	}

	private static <T> T defaultAnnotationParameter(Object defaultValue, Class<T> type) {
		Object returnValue = defaultValue;

		// resolve some mismatches between what's stored in jandex and what the defaults are for annotations
		// in case of nested annotation arrays, jandex returns arrays of AnnotationInstances, hence we return
		// an empty array of this type here
		if ( defaultValue.getClass().isArray() && defaultValue.getClass().getComponentType().isAnnotation() ) {
			returnValue = new AnnotationInstance[0];
		}
		return type.cast( returnValue );
	}

	private static <T> T explicitAnnotationParameter(AnnotationValue annotationValue, Class<T> type) {
		Object returnValue = annotationValue.value();

		// if the jandex return type is Type we actually try to retrieve a class parameter
		// for our purposes we just return the fqcn of the class
		if ( returnValue instanceof Type ) {
			returnValue = ( (Type) returnValue ).name().toString();
		}

		// arrays we have to handle explicitly
		if ( type.isArray() ) {
			AnnotationValue[] values = (AnnotationValue[]) returnValue;
			Class<?> componentType = type.getComponentType();
			Object[] arr = (Object[]) Array.newInstance( componentType, values.length );
			for ( int i = 0; i < values.length; i++ ) {
				arr[i] = componentType.cast( values[i].value() );
			}
			returnValue = arr;
		}

		return type.cast( returnValue );
	}
}
