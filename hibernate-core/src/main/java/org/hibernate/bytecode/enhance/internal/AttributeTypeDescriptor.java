/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.bytecode.enhance.internal;

import javassist.CtClass;
import javassist.CtField;
import javassist.NotFoundException;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;

import javax.persistence.Id;
import java.util.Collection;
import java.util.Locale;

/**
 * utility class to generate interceptor methods
 * @see org.hibernate.engine.spi.PersistentAttributeInterceptor
 * 
 * @author <a href="mailto:lbarreiro@redhat.com">Luis Barreiro</a>
 */
public abstract class AttributeTypeDescriptor {

	public abstract String buildReadInterceptionBodyFragment(String fieldName);

	public abstract String buildWriteInterceptionBodyFragment(String fieldName);

	public String buildInLineDirtyCheckingBodyFragment(EnhancementContext context, CtField currentValue) {
		final StringBuilder builder = new StringBuilder();
		try {
			// should ignore primary keys
			for ( Object o : currentValue.getType().getAnnotations() ) {
				if ( o instanceof Id) {
					return "";
				}
			}
			builder.append( String.format( "if (%s() != null", EnhancerConstants.INTERCEPTOR_GETTER_NAME ) );

			// primitives || enums
			if ( currentValue.getType().isPrimitive() || currentValue.getType().isEnum() ) {
				builder.append( String.format( " && %s != $1)", currentValue.getName()) );
			}
			// simple data types
			else if ( currentValue.getType().getName().startsWith( "java.lang" )
					|| currentValue.getType().getName().startsWith( "java.math.Big" )
					|| currentValue.getType().getName().startsWith( "java.sql.Time" )
					|| currentValue.getType().getName().startsWith( "java.sql.Date" )
					|| currentValue.getType().getName().startsWith( "java.util.Date" )
					|| currentValue.getType().getName().startsWith( "java.util.Calendar" ) ) {
				builder.append( String.format( "&& ((%s == null) || (!%<s.equals($1))))", currentValue.getName() ) );
			}
			// all other objects
			else {
				// if the field is a collection we return since we handle that in a separate method
				for ( CtClass ctClass : currentValue.getType().getInterfaces() ) {
					if ( ctClass.getName().equals( Collection.class.getName() ) ) {
						// if the collection is not managed we should write it to the tracker
						if ( context.isMappedCollection( currentValue ) ) {
							return "";
						}
					}
				}
				// TODO: for now just call equals, should probably do something else here
				builder.append( String.format( "&& ((%s == null) || (!%<s.equals($1))))", currentValue.getName() ) );
			}
			builder.append( String.format( " { %s(\"%s\"); }", EnhancerConstants.TRACKER_CHANGER_NAME, currentValue.getName() ) );
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		catch (NotFoundException e) {
			e.printStackTrace();
		}
		return builder.toString();
	}

	/* --- */

	/**
	 * factory method to get the AttributeTypeDescriptor for a particular field type
	 */
	public static AttributeTypeDescriptor resolve(CtField persistentField) throws NotFoundException {
		if ( persistentField.getType() == CtClass.booleanType ) {
			return new PrimitiveAttributeTypeDescriptor( Boolean.TYPE );
		}
		else if ( persistentField.getType() == CtClass.byteType ) {
			return new PrimitiveAttributeTypeDescriptor( Byte.TYPE );
		}
		else if ( persistentField.getType() == CtClass.charType ) {
			return new PrimitiveAttributeTypeDescriptor( Character.TYPE );
		}
		else if ( persistentField.getType() == CtClass.shortType ) {
			return new PrimitiveAttributeTypeDescriptor( Short.TYPE );
		}
		else if ( persistentField.getType() == CtClass.intType ) {
			return new PrimitiveAttributeTypeDescriptor( Integer.TYPE );
		}
		else if ( persistentField.getType() == CtClass.longType ) {
			return new PrimitiveAttributeTypeDescriptor( Long.TYPE );
		}
		else if ( persistentField.getType() == CtClass.doubleType ) {
			return new PrimitiveAttributeTypeDescriptor( Double.TYPE );
		}
		else if ( persistentField.getType() == CtClass.floatType ) {
			return new PrimitiveAttributeTypeDescriptor( Float.TYPE );
		}
		else {
			return new ObjectAttributeTypeDescriptor( persistentField.getType() );
		}
	}

	/* --- */

	/**
	 * AttributeTypeDescriptor for non primitive types
	 */
	private static class ObjectAttributeTypeDescriptor extends AttributeTypeDescriptor {

		private final String type;

		private ObjectAttributeTypeDescriptor(CtClass concreteType) {
			this.type = concreteType.getName();
		}

		public String buildReadInterceptionBodyFragment(String fieldName) {
			return String.format( "" +
							"if ( %3$s() != null ) {%n" +
							"  this.%1$s = (%2$s) %3$s().readObject(this, \"%1$s\", this.%1$s);%n" +
							"}",
					fieldName,
					type,
					EnhancerConstants.INTERCEPTOR_GETTER_NAME);
		}

		public String buildWriteInterceptionBodyFragment(String fieldName) {
			return String.format( "" +
							"%2$s localVar = $1;%n" +
							"if ( %3$s() != null ) {%n" +
							"  localVar = (%2$s) %3$s().writeObject(this, \"%1$s\", this.%1$s, $1);%n" +
							"}%n" +
							"this.%1$s = localVar;",
					fieldName,
					type,
					EnhancerConstants.INTERCEPTOR_GETTER_NAME);
		}
	}

	/**
	 * AttributeTypeDescriptor for primitive types
	 */
	private static class PrimitiveAttributeTypeDescriptor extends AttributeTypeDescriptor {

		private final String type;

		private PrimitiveAttributeTypeDescriptor(Class<?> primitiveType) {
			if ( !primitiveType.isPrimitive() ) {
				throw new IllegalArgumentException( "Primitive attribute type descriptor can only be used on primitive types" );
			}
			// capitalize first letter
			this.type = primitiveType.getSimpleName().substring( 0, 1 ).toUpperCase(Locale.ROOT) + primitiveType.getSimpleName().substring( 1 );
		}

		public String buildReadInterceptionBodyFragment(String fieldName) {
			return String.format( "" +
							"if (%3$s() != null ) {%n" +
							"  this.%1$s = %3$s().read%2$s(this, \"%1$s\", this.%1$s);%n" +
							"}",
					fieldName,
					type,
					EnhancerConstants.INTERCEPTOR_GETTER_NAME );
		}

		public String buildWriteInterceptionBodyFragment(String fieldName) {
			return String.format( "" +
							"%2$s localVar = $1;%n" +
							"if ( %4$s() != null ) {%n" +
							"  localVar = %4$s().write%3$s(this, \"%1$s\", this.%1$s, $1);%n" +
							"}%n" +
							"this.%1$s = localVar;",
					fieldName,
					type.toLowerCase(Locale.ROOT ),
					type,
					EnhancerConstants.INTERCEPTOR_GETTER_NAME
			);
		}
	}

}
