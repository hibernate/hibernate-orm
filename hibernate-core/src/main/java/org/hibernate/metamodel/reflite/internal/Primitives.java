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

import org.hibernate.metamodel.reflite.spi.ArrayDescriptor;
import org.hibernate.metamodel.reflite.spi.FieldDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;
import org.hibernate.metamodel.reflite.spi.Name;
import org.hibernate.metamodel.reflite.spi.PrimitiveTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.PrimitiveWrapperTypeDescriptor;

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
					new DotNameAdapter( primitiveArrayClass.getName() ),
					primitiveArrayClass.getModifiers(),
					this.primitiveType
			);
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

	public static JavaTypeDescriptor resolveByName(Name name) {
		assert name != null;

		final String typeNameString = name.fullName();

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
			return CHAR.primitiveType;
		}
		else if ( boolean.class.getName().equals( typeNameString ) ) {
			return BOOLEAN.primitiveType;
		}
		else if ( byte.class.getName().equals( typeNameString ) ) {
			return BYTE.primitiveType;
		}
		else if ( short.class.getName().equals( typeNameString ) ) {
			return SHORT.primitiveType;
		}
		else if ( int.class.getName().equals( typeNameString ) ) {
			return INTEGER.primitiveType;
		}
		else if ( long.class.getName().equals( typeNameString ) ) {
			return LONG.primitiveType;
		}
		else if ( float.class.getName().equals( typeNameString ) ) {
			return FLOAT.primitiveType;
		}
		else if ( double.class.getName().equals( typeNameString ) ) {
			return DOUBLE.primitiveType;
		}
		else if ( double.class.getName().equals( typeNameString ) ) {
			return DOUBLE.primitiveWrapperType;
		}

		return null;
	}


	private static class PrimitiveDescriptorImpl implements PrimitiveTypeDescriptor {
		private final Name name;
		private final int modifiers;
		private final PrimitiveGroup group;

		protected PrimitiveDescriptorImpl(Class clazz, PrimitiveGroup group) {
			this.name = new DotNameAdapter( clazz.getName() );
			this.modifiers = clazz.getModifiers();
			this.group = group;
		}

		@Override
		public Name getName() {
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
		public PrimitiveWrapperTypeDescriptor getWrapperTypeDescriptor() {
			return group.primitiveWrapperType;
		}
	}

	private static class WrapperDescriptorImpl implements PrimitiveWrapperTypeDescriptor {
		private final Name name;
		private final int modifiers;
		private final PrimitiveGroup group;

		private WrapperDescriptorImpl(Class clazz, PrimitiveGroup group) {
			this.name = new DotNameAdapter( clazz.getName() );
			this.modifiers = clazz.getModifiers();
			this.group = group;
		}

		@Override
		public Name getName() {
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
		public PrimitiveTypeDescriptor getPrimitiveTypeDescriptor() {
			return group.primitiveType;
		}
	}

}
