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
package org.hibernate.metamodel.reflite.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.reflite.spi.ArrayDescriptor;
import org.hibernate.metamodel.reflite.spi.FieldDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;
import org.hibernate.metamodel.reflite.spi.PrimitiveTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.PrimitiveWrapperTypeDescriptor;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 * @author Steve Ebersole
 */
public class Primitives {
	public static class PrimitiveGroup {
		private final PrimitiveTypeDescriptor primitiveType;
		private final PrimitiveWrapperTypeDescriptor primitiveWrapperType;

		private final ArrayDescriptor primitiveArrayType;

		public PrimitiveGroup(Class primitiveClass, Class primitiveArrayClass, Class wrapperClass) {
			assert primitiveClass.isPrimitive();
			assert primitiveArrayClass.isArray();
			assert !wrapperClass.isPrimitive();

			this.primitiveType = new PrimitiveDescriptorImpl( primitiveClass, this );
			this.primitiveWrapperType = new WrapperDescriptorImpl( wrapperClass, this );

			this.primitiveArrayType = new ArrayDescriptorImpl(
					DotName.createSimple( primitiveArrayClass.getName() ),
					primitiveArrayClass.getModifiers(),
					this.primitiveType
			);
		}

		public PrimitiveTypeDescriptor getPrimitiveType() {
			return primitiveType;
		}

		public PrimitiveWrapperTypeDescriptor getPrimitiveWrapperType() {
			return primitiveWrapperType;
		}

		public ArrayDescriptor getPrimitiveArrayType() {
			return primitiveArrayType;
		}
	}

	public static final PrimitiveGroup CHAR = new PrimitiveGroup( char.class, char[].class, Character.class );
	public static final PrimitiveGroup BOOLEAN = new PrimitiveGroup( boolean.class, boolean[].class,  Boolean.class );
	public static final PrimitiveGroup BYTE = new PrimitiveGroup( byte.class, byte[].class, Byte.class );
	public static final PrimitiveGroup SHORT = new PrimitiveGroup( short.class, short[].class, Short.class );
	public static final PrimitiveGroup INTEGER = new PrimitiveGroup( int.class, int[].class, Integer.class );
	public static final PrimitiveGroup LONG = new PrimitiveGroup( long.class, long[].class, Long.class );
	public static final PrimitiveGroup FLOAT = new PrimitiveGroup( float.class, float[].class, Float.class );
	public static final PrimitiveGroup DOUBLE = new PrimitiveGroup( double.class, double[].class, Double.class );

	public static JavaTypeDescriptor resolveByName(DotName name) {
		assert name != null;

		final String typeNameString = name.toString();

		if ( char.class.getName().equals( typeNameString ) ) {
			return CHAR.primitiveType;
		}
		else if ( Character.class.getName().equals( typeNameString ) ) {
			return CHAR.primitiveWrapperType;
		}
		else if ( boolean.class.getName().equals( typeNameString ) ) {
			return BOOLEAN.primitiveType;
		}
		else if ( Boolean.class.getName().equals( typeNameString ) ) {
			return BOOLEAN.primitiveWrapperType;
		}
		else if ( byte.class.getName().equals( typeNameString ) ) {
			return BYTE.primitiveType;
		}
		else if ( Byte.class.getName().equals( typeNameString ) ) {
			return BYTE.primitiveWrapperType;
		}
		else if ( short.class.getName().equals( typeNameString ) ) {
			return SHORT.primitiveType;
		}
		else if ( Short.class.getName().equals( typeNameString ) ) {
			return SHORT.primitiveArrayType;
		}
		else if ( int.class.getName().equals( typeNameString ) ) {
			return INTEGER.primitiveType;
		}
		else if ( Integer.class.getName().equals( typeNameString ) ) {
			return INTEGER.primitiveWrapperType;
		}
		else if ( long.class.getName().equals( typeNameString ) ) {
			return LONG.primitiveType;
		}
		else if ( Long.class.getName().equals( typeNameString ) ) {
			return LONG.primitiveWrapperType;
		}
		else if ( float.class.getName().equals( typeNameString ) ) {
			return FLOAT.primitiveType;
		}
		else if ( Float.class.getName().equals( typeNameString ) ) {
			return FLOAT.primitiveWrapperType;
		}
		else if ( double.class.getName().equals( typeNameString ) ) {
			return DOUBLE.primitiveType;
		}
		else if ( double.class.getName().equals( typeNameString ) ) {
			return DOUBLE.primitiveWrapperType;
		}

		return null;
	}

	public static JavaTypeDescriptor primitiveArrayDescriptor(Class type) {
		assert type != null;
		assert type.isPrimitive();

		final String typeNameString = type.getName();

		if ( char.class.getName().equals( typeNameString ) ) {
			return CHAR.primitiveArrayType;
		}
		else if ( boolean.class.getName().equals( typeNameString ) ) {
			return BOOLEAN.primitiveArrayType;
		}
		else if ( byte.class.getName().equals( typeNameString ) ) {
			return BYTE.primitiveArrayType;
		}
		else if ( short.class.getName().equals( typeNameString ) ) {
			return SHORT.primitiveArrayType;
		}
		else if ( int.class.getName().equals( typeNameString ) ) {
			return INTEGER.primitiveArrayType;
		}
		else if ( long.class.getName().equals( typeNameString ) ) {
			return LONG.primitiveArrayType;
		}
		else if ( float.class.getName().equals( typeNameString ) ) {
			return FLOAT.primitiveArrayType;
		}
		else if ( double.class.getName().equals( typeNameString ) ) {
			return DOUBLE.primitiveArrayType;
		}

		return null;
	}

	public static ArrayDescriptor primitiveArrayDescriptor(PrimitiveTypeDescriptor componentType) {
		if ( componentType == null ) {
			return null;
		}

		return ( (PrimitiveDescriptorImpl) componentType ).group.primitiveArrayType;
	}

	public static JavaTypeDescriptor decipherArrayComponentCode(char arrayComponentCode) {
		switch ( arrayComponentCode ) {
			case 'C': {
				return CHAR.primitiveType;
			}
			case 'Z': {
				return BOOLEAN.primitiveType;
			}
			case 'B': {
				return BYTE.primitiveType;
			}
			case 'S': {
				return SHORT.primitiveType;
			}
			case 'I': {
				return INTEGER.primitiveType;
			}
			case 'J': {
				return LONG.primitiveType;
			}
			case 'F': {
				return FLOAT.primitiveType;
			}
			case 'D': {
				return DOUBLE.primitiveType;
			}
			default: {
				throw new IllegalArgumentException(
						"Unexpected character passed in as primitive array component code : "
								+ arrayComponentCode
				);
			}
		}
	}

	public static void main(String... args) {
		System.out.println( "   char : " + char[].class );
		System.out.println( "boolean : " + boolean[].class );
		System.out.println( "   byte : " + byte[].class );
		System.out.println( "  short : " + short[].class );
		System.out.println( "    int : " + int[].class );
		System.out.println( "   long : " + long[].class );
		System.out.println( "  float : " + float[].class );
		System.out.println( " double : " + double[].class );
	}

	private static class PrimitiveDescriptorImpl implements PrimitiveTypeDescriptor {
		private final Class clazz;
		private final DotName name;
		private final int modifiers;
		private final PrimitiveGroup group;

		protected PrimitiveDescriptorImpl(Class clazz, PrimitiveGroup group) {
			this.clazz = clazz;
			this.name = DotName.createSimple( clazz.getName() );
			this.modifiers = clazz.getModifiers();
			this.group = group;
		}
		
		@Override
		public Class getClassType() {
			return clazz;
		}

		@Override
		public DotName getName() {
			return name;
		}

		@Override
		public int getModifiers() {
			return modifiers;
		}

		@Override
		public Collection<FieldDescriptor> getDeclaredFields() {
			return Collections.emptyList();
		}

		@Override
		public Collection<MethodDescriptor> getDeclaredMethods() {
			return Collections.emptyList();
		}

		@Override
		public AnnotationInstance findTypeAnnotation(DotName annotationType) {
			return null;
		}

		@Override
		public AnnotationInstance findLocalTypeAnnotation(DotName annotationType) {
			return null;
		}

		@Override
		public Collection<AnnotationInstance> findAnnotations(DotName annotationType) {
			return Collections.emptyList();
		}

		@Override
		public Collection<AnnotationInstance> findLocalAnnotations(DotName annotationType) {
			return Collections.emptyList();
		}

		@Override
		public boolean isAssignableFrom(JavaTypeDescriptor check) {
			if ( check == null ) {
				throw new IllegalArgumentException( "Descriptor to check cannot be null" );
			}

			if ( equals( check ) ) {
				return true;
			}

//			if ( PrimitiveWrapperTypeDescriptor.class.isInstance( check ) ) {
//				final PrimitiveWrapperTypeDescriptor wrapper = (PrimitiveWrapperTypeDescriptor) check;
//				if ( equals( wrapper.getPrimitiveTypeDescriptor() ) ) {
//					return true;
//				}
//			}
//
			return false;
		}

		@Override
		public List<JavaTypeDescriptor> getResolvedParameterTypes() {
			return Collections.emptyList();
		}

		@Override
		public ClassInfo getJandexClassInfo() {
			return null;
		}

		@Override
		public PrimitiveWrapperTypeDescriptor getWrapperTypeDescriptor() {
			return group.primitiveWrapperType;
		}
	}

	private static class WrapperDescriptorImpl implements PrimitiveWrapperTypeDescriptor {
		private final DotName name;
		private final int modifiers;
		private final PrimitiveGroup group;

		private WrapperDescriptorImpl(Class clazz, PrimitiveGroup group) {
			this.name = DotName.createSimple( clazz.getName() );
			this.modifiers = clazz.getModifiers();
			this.group = group;
		}

		@Override
		public DotName getName() {
			return name;
		}

		@Override
		public int getModifiers() {
			return modifiers;
		}

		@Override
		public Collection<FieldDescriptor> getDeclaredFields() {
			return Collections.emptyList();
		}

		@Override
		public Collection<MethodDescriptor> getDeclaredMethods() {
			return Collections.emptyList();
		}

		@Override
		public AnnotationInstance findTypeAnnotation(DotName annotationType) {
			return null;
		}

		@Override
		public AnnotationInstance findLocalTypeAnnotation(DotName annotationType) {
			return null;
		}

		@Override
		public Collection<AnnotationInstance> findAnnotations(DotName annotationType) {
			return Collections.emptyList();
		}

		@Override
		public Collection<AnnotationInstance> findLocalAnnotations(DotName annotationType) {
			return Collections.emptyList();
		}

		@Override
		public boolean isAssignableFrom(JavaTypeDescriptor check) {
			if ( check == null ) {
				throw new IllegalArgumentException( "Descriptor to check cannot be null" );
			}

			if ( equals( check ) ) {
				return true;
			}

//			if ( PrimitiveTypeDescriptor.class.isInstance( check ) ) {
//				final PrimitiveTypeDescriptor primitive = (PrimitiveTypeDescriptor) check;
//				if ( equals( primitive.getWrapperTypeDescriptor() ) ) {
//					return true;
//				}
//			}
//
			return false;
		}

		@Override
		public List<JavaTypeDescriptor> getResolvedParameterTypes() {
			return Collections.emptyList();
		}

		@Override
		public ClassInfo getJandexClassInfo() {
			return null;
		}

		@Override
		public PrimitiveTypeDescriptor getPrimitiveTypeDescriptor() {
			return group.primitiveType;
		}
	}

}
