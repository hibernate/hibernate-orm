/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
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
package org.hibernate.metamodel.source.annotations.xml.mocker;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import org.hibernate.HibernateException;
import org.hibernate.internal.jaxb.mapping.orm.JaxbCascadeType;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * @author Strong Liu
 */
public class MockHelper {

	static final AnnotationValue[] EMPTY_ANNOTATION_VALUE_ARRAY = new AnnotationValue[0];
	static final Type[] EMPTY_TYPE_ARRAY = new Type[0];

	/**
	 * util method for String Array attribute Annotation
	 *
	 * @param name
	 * @param values
	 * @param annotationValueList
	 */
	static void stringArrayValue(String name, List<String> values, List<AnnotationValue> annotationValueList) {
		if ( isNotEmpty( values ) ) {
			AnnotationValue[] annotationValues = new AnnotationValue[values.size()];
			for ( int j = 0; j < values.size(); j++ ) {
				annotationValues[j] = stringValue( "", values.get( j ) );
			}
			annotationValueList.add(
					AnnotationValue.createArrayValue(
							name, annotationValues
					)
			);
		}
	}

	/**
	 * util method for single string attribute Annotation only
	 */
	static AnnotationValue[] stringValueArray(String name, String value) {
		return nullSafe( stringValue( name, value ) );
	}

	private static AnnotationValue stringValue(String name, String value) {
		if ( StringHelper.isNotEmpty( value ) ) {
			return AnnotationValue.createStringValue( name, value );
		}
		return null;
	}

	static void stringValue(String name, String value, List<AnnotationValue> annotationValueList) {
		addToCollectionIfNotNull( annotationValueList, stringValue( name, value ) );
	}

	private static AnnotationValue integerValue(String name, Integer value) {
		if ( value == null ) {
			return null;
		}
		return AnnotationValue.createIntegerValue( name, value );
	}

	static void integerValue(String name, Integer value, List<AnnotationValue> annotationValueList) {
		addToCollectionIfNotNull( annotationValueList, integerValue( name, value ) );
	}

	static AnnotationValue[] booleanValueArray(String name, Boolean value) {
		return nullSafe( booleanValue( name, value ) );
	}

	static void booleanValue(String name, Boolean value, List<AnnotationValue> annotationValueList) {
		addToCollectionIfNotNull( annotationValueList, booleanValue( name, value ) );
	}

	private static AnnotationValue booleanValue(String name, Boolean value) {
		if ( value == null ) {
			return null;
		}
		return AnnotationValue.createBooleanValue( name, value );
	}

	private static AnnotationValue classValue(String name, String className, ServiceRegistry serviceRegistry) {
		if ( StringHelper.isNotEmpty( className ) ) {
			return AnnotationValue.createClassValue( name, getType( className, serviceRegistry ) );
		}
		return null;
	}


	static void classValue(String name, String className, List<AnnotationValue> list, ServiceRegistry serviceRegistry) {
		addToCollectionIfNotNull( list, classValue( name, className, serviceRegistry ) );
	}

	static AnnotationValue[] classValueArray(String name, String className, ServiceRegistry serviceRegistry) {
		return nullSafe( classValue( name, className, serviceRegistry ) );
	}

	static AnnotationValue nestedAnnotationValue(String name, AnnotationInstance value) {
		if ( value == null ) {
			return null;
		}
		return AnnotationValue.createNestedAnnotationValue(
				name, value
		);
	}

	static void nestedAnnotationValue(String name, AnnotationInstance value, List<AnnotationValue> list) {
		addToCollectionIfNotNull( list, nestedAnnotationValue( name, value ) );
	}

	private static AnnotationValue[] nullSafe(AnnotationValue value) {
		return value == null ? EMPTY_ANNOTATION_VALUE_ARRAY : new AnnotationValue[] {
				value
		};
	}

	static void classArrayValue(String name, List<String> classNameList, List<AnnotationValue> list, ServiceRegistry serviceRegistry) {
		if ( isNotEmpty( classNameList ) ) {

			List<AnnotationValue> clazzValueList = new ArrayList<AnnotationValue>( classNameList.size() );
			for ( String clazz : classNameList ) {
				addToCollectionIfNotNull( clazzValueList, classValue( "", clazz, serviceRegistry ) );
			}

			list.add(
					AnnotationValue.createArrayValue(
							name, toArray( clazzValueList )
					)
			);
		}
	}

	public static AnnotationValue[] toArray(List<AnnotationValue> list) {
		AnnotationValue[] values = EMPTY_ANNOTATION_VALUE_ARRAY;
		if ( isNotEmpty( list ) ) {
			values = list.toArray( new AnnotationValue[list.size()] );
		}
		return values;
	}

	private static AnnotationValue enumValue(String name, DotName typeName, Enum value) {
		if ( value != null && StringHelper.isNotEmpty( value.toString() ) ) {
			return AnnotationValue.createEnumValue( name, typeName, value.toString() );
		}
		return null;
	}

	static void cascadeValue(String name, JaxbCascadeType cascadeType, boolean isCascadePersistDefault, List<AnnotationValue> annotationValueList) {
		List<Enum> enumList = new ArrayList<Enum>();
		if ( isCascadePersistDefault ) {
			enumList.add( javax.persistence.CascadeType.PERSIST );
		}
		if ( cascadeType != null ) {
			if ( cascadeType.getCascadeAll() != null ) {
				enumList.add( javax.persistence.CascadeType.ALL );
			}
			if ( cascadeType.getCascadePersist() != null && !isCascadePersistDefault ) {
				enumList.add( javax.persistence.CascadeType.PERSIST );
			}
			if ( cascadeType.getCascadeMerge() != null ) {
				enumList.add( javax.persistence.CascadeType.MERGE );
			}
			if ( cascadeType.getCascadeRemove() != null ) {
				enumList.add( javax.persistence.CascadeType.REMOVE );
			}
			if ( cascadeType.getCascadeRefresh() != null ) {
				enumList.add( javax.persistence.CascadeType.REFRESH );
			}
			if ( cascadeType.getCascadeDetach() != null ) {
				enumList.add( javax.persistence.CascadeType.DETACH );
			}
		}
		if ( !enumList.isEmpty() ) {
			MockHelper.enumArrayValue( name, JPADotNames.CASCADE_TYPE, enumList, annotationValueList );
		}
	}

	static void enumArrayValue(String name, DotName typeName, List<Enum> valueList, List<AnnotationValue> list) {
		if ( isNotEmpty( valueList ) ) {

			List<AnnotationValue> enumValueList = new ArrayList<AnnotationValue>( valueList.size() );
			for ( Enum e : valueList ) {
				addToCollectionIfNotNull( enumValueList, enumValue( "", typeName, e ) );
			}
			list.add(
					AnnotationValue.createArrayValue(
							name, toArray( enumValueList )
					)
			);
		}
	}

	static void enumValue(String name, DotName typeName, Enum value, List<AnnotationValue> list) {
		addToCollectionIfNotNull( list, enumValue( name, typeName, value ) );
	}

	static AnnotationValue[] enumValueArray(String name, DotName typeName, Enum value) {
		return nullSafe( enumValue( name, typeName, value ) );
	}

	public static void addToCollectionIfNotNull(Collection collection, Object value) {
		if ( value != null && collection != null ) {
			collection.add( value );
		}
	}


	/**
	 * @param t1 can't be null
	 * @param t2 can't be null
	 */
	public static boolean targetEquals(AnnotationTarget t1, AnnotationTarget t2) {
		if ( t1 == t2 ) {
			return true;
		}
		if ( t1 != null && t2 != null ) {

			if ( t1.getClass() == t2.getClass() ) {
				if ( t1.getClass() == ClassInfo.class ) {
					return ( (ClassInfo) t1 ).name().equals( ( (ClassInfo) t2 ).name() );
				}
				else if ( t1.getClass() == MethodInfo.class ) {
					return ( (MethodInfo) t1 ).name().equals( ( (MethodInfo) t2 ).name() );
				}
				else {
					return ( (FieldInfo) t1 ).name().equals( ( (FieldInfo) t2 ).name() );
				}
			}
		}
		return false;
	}

	public static boolean isNotEmpty(Collection collection) {
		return collection != null && !collection.isEmpty();
	}


	static AnnotationInstance create(DotName name, AnnotationTarget target, List<AnnotationValue> annotationValueList) {
		return create(
				name, target, toArray( annotationValueList )
		);

	}

	static String buildSafeClassName(String className, String defaultPackageName) {
		if ( className.indexOf( '.' ) < 0 && StringHelper.isNotEmpty( defaultPackageName ) ) {
			className = StringHelper.qualify( defaultPackageName, className );
		}
		return className;
	}

	static AnnotationInstance create(DotName name, AnnotationTarget target, AnnotationValue[] values) {
		if ( values == null || values.length == 0 ) {
			values = EMPTY_ANNOTATION_VALUE_ARRAY;
		}
		return AnnotationInstance.create( name, target, addMockMark( values ) );
	}

	private static AnnotationValue[] addMockMark(AnnotationValue[] values) {
		AnnotationValue[] newValues = new AnnotationValue[values.length + 1];
		System.arraycopy( values, 0, newValues, 0, values.length );
		newValues[values.length] = booleanValue( "isMocked", true );
		return newValues;
	}


	private static MethodInfo getMethodInfo(ClassInfo classInfo, Method method) {
		Class returnTypeClass = method.getReturnType();
		short access_flags = (short) method.getModifiers();
		return MethodInfo.create(
				classInfo,
				method.getName(),
				getTypes( method.getParameterTypes() ),
				getType( returnTypeClass ),
				access_flags
		);
	}

	enum TargetType {METHOD, FIELD, PROPERTY}

	static AnnotationTarget getTarget(ServiceRegistry serviceRegistry, ClassInfo classInfo, String name, TargetType type) {
		Class clazz = serviceRegistry.getService( ClassLoaderService.class ).classForName( classInfo.toString() );
		switch ( type ) {
			case FIELD:
				Field field = getField( clazz, name );
				if ( field == null ) {
					throw new HibernateException(
							"Unable to load field "
									+ name
									+ " of class " + clazz.getName()
					);
				}

				return FieldInfo.create(
						classInfo, name, getType( field.getType() ), (short) ( field.getModifiers() )
				);
			case METHOD:
				Method method = getMethod( clazz, name );
				if ( method == null ) {
					throw new HibernateException(
							"Unable to load method "
									+ name
									+ " of class " + clazz.getName()
					);
				}
				return getMethodInfo( classInfo, method );
			case PROPERTY:
				method = getterMethod( clazz, name );
				if ( method == null ) {
					throw new HibernateException(
							"Unable to load property "
									+ name
									+ " of class " + clazz.getName()
					);
				}
				return getMethodInfo( classInfo, method );

		}
		throw new HibernateException( "" );
	}

	//copied from org.hibernate.internal.util.ReflectHelper
	private static Method getterMethod(Class theClass, String propertyName) {
		Method[] methods = theClass.getDeclaredMethods();
		Method.setAccessible( methods, true );
		for ( Method method : methods ) {
			// if the method has parameters, skip it
			if ( method.getParameterTypes().length != 0 ) {
				continue;
			}
			// if the method is a "bridge", skip it
			if ( method.isBridge() ) {
				continue;
			}

			final String methodName = method.getName();

			// try "get"
			if ( methodName.startsWith( "get" ) || methodName.startsWith( "has" ) ) {
				String testStdMethod = Introspector.decapitalize( methodName.substring( 3 ) );
				String testOldMethod = methodName.substring( 3 );
				if ( testStdMethod.equals( propertyName ) || testOldMethod.equals( propertyName ) ) {
					return method;
				}
			}

			// if not "get", then try "is"
			if ( methodName.startsWith( "is" ) ) {
				String testStdMethod = Introspector.decapitalize( methodName.substring( 2 ) );
				String testOldMethod = methodName.substring( 2 );
				if ( testStdMethod.equals( propertyName ) || testOldMethod.equals( propertyName ) ) {
					return method;
				}
			}
		}

		return null;
	}


	private static Method getMethod(Class theClass, String propertyName) {
		Method[] methods = theClass.getDeclaredMethods();
		Method.setAccessible( methods, true );
		for ( Method method : methods ) {
			// if the method has parameters, skip it
			if ( method.getParameterTypes().length != 0 ) {
				continue;
			}
			// if the method is a "bridge", skip it
			if ( method.isBridge() ) {
				continue;
			}

			final String methodName = method.getName();
			if ( methodName.equals( propertyName ) ) {
				return method;
			}
		}

		return null;
	}

	private static Field getField(Class clazz, String name) {
		Field[] fields = clazz.getDeclaredFields();
		Field.setAccessible( fields, true );
		for ( Field field : fields ) {
			if ( field.getName().equals( name ) ) {
				return field;
			}
		}
		return null;
	}

	private static Type[] getTypes(Class[] classes) {
		if ( classes == null || classes.length == 0 ) {
			return EMPTY_TYPE_ARRAY;
		}
		Type[] types = new Type[classes.length];
		for ( int i = 0; i < types.length; i++ ) {
			types[i] = getType( classes[i] );
		}
		return types;
	}


	private static Type getType(String className, ServiceRegistry serviceRegistry) {
		return getType( serviceRegistry.getService( ClassLoaderService.class ).classForName( className ) );
	}

	private static Type getType(Class clazz) {
		return Type.create( DotName.createSimple( clazz.getName() ), getTypeKind( clazz ) );
	}

	private static Type.Kind getTypeKind(Class clazz) {
		Type.Kind kind;
		if ( clazz == Void.TYPE ) {
			kind = Type.Kind.VOID;
		}
		else if ( clazz.isPrimitive() ) {
			kind = Type.Kind.PRIMITIVE;
		}
		else if ( clazz.isArray() ) {
			kind = Type.Kind.ARRAY;
		}
		else {
			kind = Type.Kind.CLASS;
		}
		return kind;
	}

}
