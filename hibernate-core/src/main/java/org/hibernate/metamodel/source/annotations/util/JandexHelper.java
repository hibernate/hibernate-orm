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
package org.hibernate.metamodel.source.annotations.util;

import java.beans.Introspector;
import java.io.IOException;
import java.io.InputStream;
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

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * Utility methods for working with the jandex annotation index.
 *
 * @author Hardy Ferentschik
 */
public class JandexHelper {

    private static final AnnotationInstance[] EMPTY_ANNOTATIONS_ARRAY = new AnnotationInstance[0];

    private static final Map<String, Object> DEFAULT_VALUES_BY_ELEMENT = new HashMap<String, Object>();

    private static Object getDefaultValue( Index index,
                                           AnnotationInstance annotation,
                                           String element ) {
        String name = annotation.name().toString();
        String fqElement = name + '.' + element;
        Object val = DEFAULT_VALUES_BY_ELEMENT.get(fqElement);
        if (val != null) return val;
        try {
            val = Index.class.getClassLoader().loadClass(name).getMethod(element).getDefaultValue();
            DEFAULT_VALUES_BY_ELEMENT.put(fqElement, val);
            return val == null ? null : val;
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new AnnotationException(error);
        }
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
            String methodName = ( (MethodInfo) target ).name();
            if ( methodName.startsWith( "is" ) ) {
                methodName = Introspector.decapitalize( methodName.substring( 2 ) );
            }
            else if ( methodName.startsWith( "has" ) ) {
                methodName = Introspector.decapitalize( methodName.substring( 3 ) );
            }
            else if ( methodName.startsWith( "get" ) ) {
                methodName = Introspector.decapitalize( methodName.substring( 3 ) );
            }
            else {
                throw new AssertionFailure( "Expected a method following the Java Bean notation" );
            }
            return methodName;
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
     * Retrieves a jandex annotation element value.  If the value is <code>null</code>, the default value specified in the
     * annotation class is retrieved instead.  Note, {@link #getValueAsEnum(Index, AnnotationInstance, String, Class)} must be
     * called to retrieve an enumerated value, and {@link #getValueAsArray(AnnotationInstance, String)} must be called to retrieve
     * an object array (other than a String array).
     *
     * @param index the jandex index containing the supplied annotation
     * @param annotation the annotation containing the element with the supplied name
     * @param element the name of the element value to be retrieve
     * @return the value if not <code>null</code>, else the default value if not
     *         <code>null</code>, else <code>null</code>.
     */
    public static Object getValue( Index index,
                                   AnnotationInstance annotation,
                                   String element ) {
        AnnotationValue val = annotation.value(element);
        if (val == null) return getDefaultValue(index, annotation, element);
        return val.asNested();
    }

    /**
     * Retrieves a jandex annotation element array.  Note, {@link #getValue(Index, AnnotationInstance, String)} may be
     * called to retrieve a String array (or a non-array value).
     *
     * @param annotation the jandex annotation containing the element with the supplied name
     * @param element the name of the element array
     * @return the element array if not <code>null</code>, else an empty array
     */
    public static AnnotationInstance[] getValueAsArray( AnnotationInstance annotation,
                                                        String element ) {
        AnnotationValue val = annotation.value(element);
        return val == null ? EMPTY_ANNOTATIONS_ARRAY : val.asNestedArray();
    }

    /**
     * Retrieves a jandex annotation element value, converting it to the supplied enumerated type.  If the value is
     * <code>null</code>, the default value specified in the annotation class is retrieved instead.
     *
     * @param <T> an enumerated type
     * @param index the jandex index containing the supplied annotation
     * @param annotation the annotation containing the enumerated element with the supplied name
     * @param element the name of the enumerated element value to be retrieve
     * @param type the type to which to convert the value before being returned
     * @return the value converted to the supplied enumerated type if the value is not <code>null</code>, else the default value if
     *         not <code>null</code>, else <code>null</code>.
     * @see #getValue(Index, AnnotationInstance, String)
     */
    public static <T extends Enum<T>> T getValueAsEnum( Index index,
                                                        AnnotationInstance annotation,
                                                        String element,
                                                        Class<T> type ) {
        AnnotationValue val = annotation.value(element);
        if (val == null) return (T)getDefaultValue(index, annotation, element);
        return Enum.valueOf(type, val.asEnum());
    }

    /**
     * Retrieves a jandex annotation element value as an Integer.  If the value is <code>null</code>, the default value specified in
     * the annotation class is retrieved instead.
     *
     * @param index the jandex index containing the supplied annotation
     * @param annotation the annotation containing the element with the supplied name
     * @param element the name of the element value to be retrieve
     * @return the value converted to an int if the value is not <code>null</code>, else the default value if not
     *         <code>null</code>, else <code>0</code>.
     */
    public static int getValueAsInt( Index index,
                                     AnnotationInstance annotation,
                                     String element ) {
        AnnotationValue val = annotation.value(element);
        if (val == null) return (Integer)getDefaultValue(index, annotation, element);
        return val.asInt();
    }

    /**
     * Retrieves a jandex annotation element value as a String.  If the value is <code>null</code>, the default value specified in
     * the annotation class is retrieved instead.
     *
     * @param index the jandex index containing the supplied annotation
     * @param annotation the annotation containing the element with the supplied name
     * @param element the name of the element value to be retrieve
     * @return the value converted to a String if the value is not <code>null</code>, else the default value if not
     *         <code>null</code>, else <code>null</code>.
     */
    public static String getValueAsString( Index index,
                                           AnnotationInstance annotation,
                                           String element ) {
        AnnotationValue val = annotation.value(element);
        if (val == null) return (String)getDefaultValue(index, annotation, element);
        return val.asString();
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

    public static Map<DotName, List<AnnotationInstance>> getTypeAnnotations(ClassInfo classInfo) {
        if ( classInfo == null ) {
            throw new IllegalArgumentException( "classInfo cannot be null" );
        }

        Map<DotName, List<AnnotationInstance>> annotations = new HashMap<DotName, List<AnnotationInstance>>();
        for ( List<AnnotationInstance> annotationList : classInfo.annotations().values() ) {
            for ( AnnotationInstance instance : annotationList ) {
                if ( instance.target() instanceof ClassInfo ) {
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

    private JandexHelper() {
    }
}
