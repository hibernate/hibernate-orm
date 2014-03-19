/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.annotations;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.metamodel.spi.ClassLoaderAccess;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

/**
 * @author Steve Ebersole
 */
public class JandexAccessImpl implements JandexAccess {
	private final IndexView index;
	private final ClassLoaderAccess classLoaderAccess;

	public JandexAccessImpl(
			IndexView index,
			ClassLoaderAccess classLoaderAccess) {
		this.index = index;
		this.classLoaderAccess = classLoaderAccess;
	}

	@Override
	public IndexView getIndex() {
		return index;
	}

	@Override
	public ClassInfo getClassInfo(String className) {
		DotName dotName = DotName.createSimple( className );
		return index.getClassByName( dotName );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> TypedValueExtractor<T> getTypedValueExtractor(Class<T> type) {
		if ( boolean.class.equals( type ) || Boolean.class.equals( type ) ) {
			return (TypedValueExtractor<T>) booleanExtractor;
		}
		else if ( byte.class.equals( type ) || Byte.class.equals( type ) ) {
			return (TypedValueExtractor<T>) byteExtractor;
		}
		else if ( char.class.equals( type ) || Character.class.equals( type ) ) {
			return (TypedValueExtractor<T>) characterExtractor;
		}
		else if ( short.class.equals( type ) || Short.class.equals( type ) ) {
			return (TypedValueExtractor<T>) shortExtractor;
		}
		else if ( int.class.equals( type ) || Integer.class.equals( type ) ) {
			return (TypedValueExtractor<T>) integerExtractor;
		}
		else if ( long.class.equals( type ) || Long.class.equals( type ) ) {
			return (TypedValueExtractor<T>) longExtractor;
		}
		else if ( float.class.equals( type ) || Float.class.equals( type ) ) {
			return (TypedValueExtractor<T>) floatExtractor;
		}
		else if ( double.class.equals( type ) || Double.class.equals( type ) ) {
			return (TypedValueExtractor<T>) doubleExtractor;
		}
		else if ( String.class.equals( type ) ) {
			return (TypedValueExtractor<T>) stringExtractor;
		}
		else if ( String[].class.equals( type ) ) {
			return (TypedValueExtractor<T>) stringArrayExtractor;
		}
		else if ( type.isEnum() ) {
			return new EnumExtractor( type );
		}
		else if ( type.isArray() && type.getComponentType().isEnum() ) {
			return new EnumArrayExtractor( type );
		}
		else if ( Type.class.equals( type ) ) {
			return (TypedValueExtractor<T>) classTypeExtractor;
		}
		else if ( type.isArray() && Type.class.equals( type.getComponentType() ) ) {
			return (TypedValueExtractor<T>) classTypeArrayExtractor;
		}
		else if ( AnnotationInstance.class.equals( type ) ) {
			return (TypedValueExtractor<T>) nestedExtractor;
		}
		else if ( type.isArray() && AnnotationInstance.class.equals( type.getComponentType() ) ) {
			return (TypedValueExtractor<T>) nestedArrayExtractor;
		}

		// checks for some specific unsupported types
		if ( Class.class.equals( type ) || Class[].class.equals( type ) ) {
			throw new IllegalArgumentException(
					"Class and Class[] typed annotation attributes should be extracted" +
							"by FQN or by org.jboss.jandex.Type"
			);
		}

		throw new IllegalArgumentException(
				"Unsupported extraction type [" + type.getName() + "] requested"
		);
	}


	private abstract class AbstractTypedValueExtractor<T> implements TypedValueExtractor<T> {
		@Override
		public T extract(AnnotationInstance annotationInstance, String name) {
			final AnnotationValue attributeInstance = annotationInstance.value( name );
			if ( attributeInstance == null ) {
				return interpretDefaultValue( getDefaultValue( annotationInstance, name ) );
			}
			else {
				return extract( attributeInstance );
			}
		}

		protected abstract T interpretDefaultValue(Object defaultValue);

		protected abstract T extract(AnnotationValue attributeInstance);

		@Override
		public T extract(AnnotationInstance annotationInstance, String name, T defaultValue) {
			final AnnotationValue attributeInstance = annotationInstance.value( name );
			if ( attributeInstance == null ) {
				return defaultValue;
			}
			else {
				return extract( attributeInstance );
			}
		}
	}


	public final TypedValueExtractor<Boolean> booleanExtractor  = new AbstractTypedValueExtractor<Boolean>() {
		@Override
		public Boolean extract(AnnotationValue value) {
			return value.asBoolean();
		}

		@Override
		public Boolean interpretDefaultValue(Object value) {
			if ( boolean.class.isInstance( value ) || Boolean.class.isInstance( value ) ) {
				return (Boolean) value;
			}
			throw new IllegalArgumentException( "Cannot convert given value [" + value + "] to Boolean" );
		}
	};

	public final TypedValueExtractor<Byte> byteExtractor = new AbstractTypedValueExtractor<Byte>() {
		@Override
		public Byte extract(AnnotationValue value) {
			return value.asByte();
		}

		@Override
		public Byte interpretDefaultValue(Object value) {
			if ( byte.class.isInstance( value ) || Byte.class.isInstance( value ) ) {
				return (Byte) value;
			}
			throw new IllegalArgumentException( "Cannot convert given value [" + value + "] to Byte" );
		}
	};

	public final TypedValueExtractor<Character> characterExtractor = new AbstractTypedValueExtractor<Character>() {
		@Override
		public Character extract(AnnotationValue value) {
			return value.asChar();
		}

		@Override
		public Character interpretDefaultValue(Object value) {
			if ( char.class.isInstance( value ) || Character.class.isInstance( value ) ) {
				return (Character) value;
			}
			throw new IllegalArgumentException( "Cannot convert given value [" + value + "] to Character" );
		}
	};

	public final TypedValueExtractor<Short> shortExtractor = new AbstractTypedValueExtractor<Short>() {
		@Override
		public Short extract(AnnotationValue value) {
			return value.asShort();
		}

		@Override
		public Short interpretDefaultValue(Object value) {
			if ( short.class.isInstance( value ) || Short.class.isInstance( value ) ) {
				return (Short) value;
			}
			throw new IllegalArgumentException( "Cannot convert given value [" + value + "] to Short" );
		}
	};

	public final TypedValueExtractor<Integer> integerExtractor = new AbstractTypedValueExtractor<Integer>() {
		@Override
		public Integer extract(AnnotationValue value) {
			return value.asInt();
		}

		@Override
		public Integer interpretDefaultValue(Object value) {
			if ( int.class.isInstance( value ) || Integer.class.isInstance( value ) ) {
				return (Integer) value;
			}
			throw new IllegalArgumentException( "Cannot convert given value [" + value + "] to Integer" );
		}
	};

	public final TypedValueExtractor<Long> longExtractor = new AbstractTypedValueExtractor<Long>() {
		@Override
		public Long extract(AnnotationValue value) {
			return value.asLong();
		}

		@Override
		public Long interpretDefaultValue(Object value) {
			if ( long.class.isInstance( value ) || Long.class.isInstance( value ) ) {
				return (Long) value;
			}
			throw new IllegalArgumentException( "Cannot convert given value [" + value + "] to Long" );
		}
	};

	public final TypedValueExtractor<Double> doubleExtractor = new AbstractTypedValueExtractor<Double>() {
		@Override
		public Double extract(AnnotationValue value) {
			return value.asDouble();
		}

		@Override
		public Double interpretDefaultValue(Object value) {
			if ( double.class.isInstance( value ) || Double.class.isInstance( value ) ) {
				return (Double) value;
			}
			throw new IllegalArgumentException( "Cannot convert given value [" + value + "] to Double" );
		}
	};

	public final TypedValueExtractor<Float> floatExtractor = new AbstractTypedValueExtractor<Float>() {
		@Override
		public Float extract(AnnotationValue value) {
			return value.asFloat();
		}

		@Override
		public Float interpretDefaultValue(Object value) {
			if ( float.class.isInstance( value ) || Float.class.isInstance( value ) ) {
				return (Float) value;
			}
			throw new IllegalArgumentException( "Cannot convert given value [" + value + "] to Float" );
		}
	};

	public final TypedValueExtractor<String> stringExtractor = new AbstractTypedValueExtractor<String>() {
		@Override
		public String extract(AnnotationValue value) {
			return value.asString();
		}

		@Override
		public String interpretDefaultValue(Object value) {
			if ( value == null ) {
				return null;
			}
			if ( String.class.isInstance( value ) ) {
				return (String) value;
			}
			throw new IllegalArgumentException( "Cannot convert given value [" + value + "] to String" );
		}
	};

	public final TypedValueExtractor<String[]> stringArrayExtractor = new AbstractTypedValueExtractor<String[]>() {
		@Override
		public String[] extract(AnnotationValue value) {
			return value.asStringArray();
		}

		@Override
		public String[] interpretDefaultValue(Object value) {
			if ( value == null ) {
				return null;
			}
			if ( String[].class.isInstance( value ) ) {
				return (String[]) value;
			}
			throw new IllegalArgumentException( "Cannot convert given value [" + value + "] to String[]" );
		}
	};

	public final TypedValueExtractor<String> enumNameExtractor = new AbstractTypedValueExtractor<String>() {
		@Override
		public String extract(AnnotationValue value) {
			return value.asEnum();
		}

		@Override
		public String interpretDefaultValue(Object value) {
			if ( Enum.class.isInstance( value ) ) {
				return ( (Enum) value ).name();
			}
			throw new IllegalArgumentException( "Cannot convert given value [" + value + "] to Enum (to extract name)" );
		}
	};

	public final TypedValueExtractor<String[]> enumNameArrayExtractor = new AbstractTypedValueExtractor<String[]>() {
		@Override
		public String[] extract(AnnotationValue value) {
			return value.asEnumArray();
		}

		@Override
		public String[] interpretDefaultValue(Object value) {
			if ( Enum[].class.isInstance( value ) ) {
				final Enum[] enums = (Enum[]) value;
				final String[] names = new String[enums.length];
				for ( int i = 0, length = enums.length; i < length; i++ ) {
					names[i] = enums[i].name();
				}
				return names;
			}
			throw new IllegalArgumentException( "Cannot convert given value [" + value + "] to Enum[] (to extract names)" );
		}
	};

	public final AbstractTypedValueExtractor<String> classNameExtractor = new AbstractTypedValueExtractor<String>() {
		@Override
		public String extract(AnnotationValue value) {
			return ( (AbstractTypedValueExtractor<String>) stringExtractor ).extract( value );
		}

		@Override
		public String interpretDefaultValue(Object value) {
			if ( value == null ) {
				return null;
			}
			if ( Class.class.isInstance( value ) ) {
				return ( (Class) value ).getName();
			}
			else if ( String.class.isInstance( value ) ) {
				return (String) value;
			}
			throw new IllegalArgumentException( "Cannot convert given value [" + value + "] to Class (to extract name)" );
		}
	};

	public final TypedValueExtractor<String[]> classNameArrayExtractor = new AbstractTypedValueExtractor<String[]>() {
		@Override
		public String[] extract(AnnotationValue value) {
			return value.asStringArray();
		}

		@Override
		public String[] interpretDefaultValue(Object value) {
			if ( Class[].class.isInstance( value ) ) {
				final Class[] types = (Class[]) value;
				final String[] names = new String[types.length];
				for ( int i = 0, length = types.length; i < length; i++ ) {
					names[i] = types[i].getName();
				}
				return names;
			}
			throw new IllegalArgumentException( "Cannot convert given value [" + value + "] to Class[] (to extract names)" );
		}
	};

	public final TypedValueExtractor<Type> classTypeExtractor = new AbstractTypedValueExtractor<Type>() {
		@Override
		public Type extract(AnnotationValue value) {
			return value.asClass();
		}

		@Override
		public Type interpretDefaultValue(Object value) {
			// What is the relationship between Type and ClassInfo?  is there one?
			// How does one get a Type reference from the index?
			return null;
		}
	};

	public final TypedValueExtractor<Type[]> classTypeArrayExtractor = new AbstractTypedValueExtractor<Type[]>() {
		@Override
		public Type[] extract(AnnotationValue value) {
			return value.asClassArray();
		}

		@Override
		public Type[] interpretDefaultValue(Object value) {
			throw new IllegalStateException(
					"Cannot interpret default Class values as Jandex Types atm, " +
							"as this class does not have access to the Jandex Index"
			);
		}
	};

	public final TypedValueExtractor<AnnotationInstance> nestedExtractor = new AbstractTypedValueExtractor<AnnotationInstance>() {
		@Override
		public AnnotationInstance extract(AnnotationValue value) {
			return value.asNested();
		}

		@Override
		public AnnotationInstance interpretDefaultValue(Object value) {
			return null;
		}
	};

	public final TypedValueExtractor<AnnotationInstance[]> nestedArrayExtractor = new AbstractTypedValueExtractor<AnnotationInstance[]>() {
		@Override
		public AnnotationInstance[] extract(AnnotationValue value) {
			return value.asNestedArray();
		}

		@Override
		public AnnotationInstance[] interpretDefaultValue(Object value) {
			return new AnnotationInstance[0];
		}
	};

	private class EnumExtractor<T extends Enum<T>> extends AbstractTypedValueExtractor<T> {
		private final Class<T> enumType;

		private EnumExtractor(Class<T> enumType) {
			this.enumType = enumType;
		}

		@Override
		@SuppressWarnings("unchecked")
		protected T interpretDefaultValue(Object defaultValue) {
			// defaultValue should be an enum already..
			if ( enumType.isInstance( defaultValue ) ) {
				return (T) defaultValue;
			}
			else if ( defaultValue instanceof String ) {
				return (T) Enum.valueOf( enumType, (String) defaultValue );
			}
			throw new IllegalArgumentException(
					"Do not know how to convert given value [" + defaultValue +
							"] to specified enum [" + enumType.getName() + "]"
			);
		}

		@Override
		protected T extract(AnnotationValue attributeInstance) {
			return Enum.valueOf(
					enumType,
					( (AbstractTypedValueExtractor<String>) enumNameExtractor ).extract( attributeInstance )
			);
		}
	}

	private class EnumArrayExtractor<T extends Enum<T>> extends AbstractTypedValueExtractor<T[]> {
		private final Class<T> enumType;

		private EnumArrayExtractor(Class<T> enumType) {
			this.enumType = enumType;
		}

		@Override
		@SuppressWarnings("unchecked")
		protected T[] interpretDefaultValue(Object defaultValue) {
			// defaultValue should be an enum array already..
			if ( defaultValue == null ) {
				return (T[]) Array.newInstance( enumType, 0 );
			}
			else if ( defaultValue.getClass().isArray()
					&& defaultValue.getClass().getComponentType().equals( enumType ) ) {
				return (T[]) defaultValue;
			}
			else if ( defaultValue.getClass().isArray()
					&& defaultValue.getClass().getComponentType().equals( String.class ) ) {
				final String[] strings = (String[]) defaultValue;
				final T[] result = (T[]) Array.newInstance( enumType, strings.length );
				for ( int i = 0; i < strings.length; i++ ) {
					result[i] = Enum.valueOf( enumType, strings[i] );
				}
				return result;
			}
			throw new IllegalArgumentException(
					"Do not know how to convert given value [" + defaultValue +
							"] to specified enum array [" + enumType.getName() + "]"
			);
		}

		@Override
		@SuppressWarnings("unchecked")
		protected T[] extract(AnnotationValue attributeInstance) {
			final String[] enumValues = ( (AbstractTypedValueExtractor<String[]>) enumNameArrayExtractor ).extract( attributeInstance );
			final T[] result = (T[]) Array.newInstance( enumType, enumValues.length );
			for ( int i = 0; i < enumValues.length; i++ ) {
				result[i] = Enum.valueOf( enumType, enumValues[i] );
			}
			return result;
		}
	}

	private static final Map<DotName, Map<String,Object>> DEFAULT_VALUES_MAP = new HashMap<DotName, Map<String,Object>>();

	private Object getDefaultValue(AnnotationInstance annotation, String attributeName) {
		final String annotationFqn = annotation.name().toString();
		Map<String,Object> annotationValueMap = DEFAULT_VALUES_MAP.get( annotation.name() );
		if ( annotationValueMap == null ) {
			// we have not processed this annotation yet
			final Class annotationClass = classLoaderAccess.classForName( annotationFqn );
			annotationValueMap = parseAttributeDefaults( annotationClass );
			DEFAULT_VALUES_MAP.put( annotation.name(), annotationValueMap );
		}

		final Object value = annotationValueMap.get( attributeName );
		if ( value == null ) {
			if ( !annotationValueMap.containsKey( attributeName ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Annotation [%s] does not define an attribute named '%s'",
								annotationFqn,
								attributeName
						)
				);
			}
		}
		return value;
	}

	private Map<String, Object> parseAttributeDefaults(Class annotationClass) {
		final Map<String,Object> results = new HashMap<String, Object>();

		final Method[] methods = annotationClass.getDeclaredMethods();
		if ( methods != null ) {
			for ( Method method : methods ) {
				Object defaultValue = method.getDefaultValue();
				if ( defaultValue != null ) {
					if ( defaultValue instanceof String ) {
						if ( "".equals( defaultValue ) ) {
							defaultValue = null;
						}
					}
					else if ( defaultValue instanceof Class ) {
						final Class clazz = (Class) defaultValue;
						if ( void.class.equals( clazz ) ) {
							defaultValue = null;
						}
						else {
							// use its FQN (ideally we'd use the jandex Type)
							defaultValue = ( (Class) defaultValue ).getName();
						}
					}
					else if ( defaultValue instanceof Class[] ) {
						// use its FQN (ideally we'd use the jandex Type)
						final Class[] types = (Class[]) defaultValue;
						final String[] fqns = new String[ types.length ];
						for ( int i = 0; i < types.length; i++ ) {
							fqns[i] = types[i].getName();
						}
						defaultValue = fqns;
					}
					else if ( defaultValue.getClass().isAnnotation() ) {
						// I can't think of a single instance where the
						// default for a nested annotation attribute is
						// anything other than an empty annotation instance
						defaultValue = null;
					}
					else if ( defaultValue.getClass().isArray()
							&& defaultValue.getClass().getComponentType().isAnnotation() ) {
						defaultValue = null;
					}
				}
				results.put( method.getName(), defaultValue );
			}
		}

		return results;
	}
}
