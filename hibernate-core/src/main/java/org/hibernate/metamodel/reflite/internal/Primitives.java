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

import org.hibernate.metamodel.reflite.spi.ArrayTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.Name;
import org.hibernate.metamodel.reflite.spi.PrimitiveTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.PrimitiveWrapperTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.TypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class Primitives {
	public static class PrimitiveGroup {
		private final PrimitiveTypeDescriptor primitiveType;
		private final PrimitiveWrapperTypeDescriptor primitiveWrapperType;

		private final ArrayTypeDescriptor primitiveArrayType;
		private final ArrayTypeDescriptor primitiveWrapperArrayType;

		public PrimitiveGroup(Class primitiveClass, Class wrapperClass) {
			assert primitiveClass.isPrimitive();
			assert !wrapperClass.isPrimitive();

			this.primitiveType = new PrimitiveDescriptorImpl( primitiveClass.getName(), this );
			this.primitiveWrapperType = new WrapperDescriptorImpl( wrapperClass.getName(), this );

			this.primitiveArrayType = new ArrayTypeDescriptorImpl( null, this.primitiveType );
			this.primitiveWrapperArrayType = new ArrayTypeDescriptorImpl( null, this.primitiveWrapperType );
		}

		public PrimitiveTypeDescriptor getPrimitiveType() {
			return primitiveType;
		}

		public PrimitiveWrapperTypeDescriptor getPrimitiveWrapperType() {
			return primitiveWrapperType;
		}

		public ArrayTypeDescriptor getPrimitiveArrayType() {
			return primitiveArrayType;
		}

		public ArrayTypeDescriptor getPrimitiveWrapperArrayType() {
			return primitiveWrapperArrayType;
		}
	}

	public static final PrimitiveGroup CHAR = new PrimitiveGroup( char.class, Character.class );
	public static final PrimitiveGroup BOOLEAN = new PrimitiveGroup( boolean.class, Boolean.class );
	public static final PrimitiveGroup BYTE = new PrimitiveGroup( byte.class, Byte.class );
	public static final PrimitiveGroup SHORT = new PrimitiveGroup( short.class, Short.class );
	public static final PrimitiveGroup INTEGER = new PrimitiveGroup( int.class, Integer.class );
	public static final PrimitiveGroup LONG = new PrimitiveGroup( long.class, Long.class );
	public static final PrimitiveGroup FLOAT = new PrimitiveGroup( float.class, Float.class );
	public static final PrimitiveGroup DOUBLE = new PrimitiveGroup( double.class, Double.class );

	public static TypeDescriptor resolveByName(Name name) {
		assert name != null;

		final String typeNameString = name.toString();

		if ( char.class.getName().equals( typeNameString ) ) {
			return CHAR.primitiveType;
		}
		else if ( Character.class.getName().equals( name ) ) {
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

	private static class PrimitiveDescriptorImpl implements PrimitiveTypeDescriptor {
		private final Name name;
		private final PrimitiveGroup group;

		protected PrimitiveDescriptorImpl(String simpleName, PrimitiveGroup group) {
			this.name = new DotNameAdapter( simpleName );
			this.group = group;
		}

		@Override
		public Name getName() {
			return name;
		}

		@Override
		public PrimitiveWrapperTypeDescriptor getWrapperTypeDescriptor() {
			return group.primitiveWrapperType;
		}

		@Override
		public boolean isVoid() {
			return false;
		}

		@Override
		public boolean isInterface() {
			return false;
		}

		@Override
		public boolean isPrimitive() {
			return true;
		}

		@Override
		public boolean isArray() {
			return false;
		}
	}

	private static class WrapperDescriptorImpl implements PrimitiveWrapperTypeDescriptor {
		private final Name name;
		private final PrimitiveGroup group;

		private WrapperDescriptorImpl(String simpleName, PrimitiveGroup group) {
			this.name = new DotNameAdapter( simpleName );
			this.group = group;
		}

		@Override
		public Name getName() {
			return name;
		}

		@Override
		public PrimitiveTypeDescriptor getPrimitiveTypeDescriptor() {
			return group.primitiveType;
		}

		@Override
		public boolean isInterface() {
			return false;
		}

		@Override
		public boolean isVoid() {
			return false;
		}

		@Override
		public boolean isArray() {
			return false;
		}

		@Override
		public boolean isPrimitive() {
			return false;
		}
	}

}
