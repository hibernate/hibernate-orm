/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.javassist;

import java.util.Collection;
import java.util.Locale;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;

import javassist.CtClass;
import javassist.CtField;
import javassist.NotFoundException;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.internal.util.compare.EqualsHelper;

/**
 * utility class to generate interceptor methods
 * @see org.hibernate.engine.spi.PersistentAttributeInterceptor
 * 
 * @author <a href="mailto:lbarreiro@redhat.com">Luis Barreiro</a>
 */
public abstract class AttributeTypeDescriptor {

	protected InheritanceMetadata inheritanceMetadata;

	protected AttributeTypeDescriptor(InheritanceMetadata inheritanceMetadata) {
		this.inheritanceMetadata = inheritanceMetadata;
	}

	public abstract String buildReadInterceptionBodyFragment(String fieldName);

	public abstract String buildWriteInterceptionBodyFragment(String fieldName);

	public String buildInLineDirtyCheckingBodyFragment(JavassistEnhancementContext context, CtField currentValue) {
		StringBuilder builder = new StringBuilder();
		try {
			// should ignore primary keys
			if ( PersistentAttributesHelper.hasAnnotation( currentValue, Id.class )
					|| PersistentAttributesHelper.hasAnnotation( currentValue, EmbeddedId.class ) ) {
				return "";
			}

			String readFragment = inheritanceMetadata.isInherited() && !inheritanceMetadata.isVisible()
					? "super." + inheritanceMetadata.getReaderName() + "()"
					: "this." + currentValue.getName();

			if ( currentValue.getType().isPrimitive() || currentValue.getType().isEnum() ) {
				// primitives || enums
				builder.append( String.format( "  if ( %s != $1 )", readFragment ) );
			}
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
				builder.append(
						String.format(
								"  if ( !%s.areEqual( %s, $1 ) )",
								EqualsHelper.class.getName(),
								readFragment
						)
				);
			}
			builder.append( String.format( "  {  %s(\"%s\");  }", EnhancerConstants.TRACKER_CHANGER_NAME, currentValue.getName() ) );
		}
		catch (NotFoundException ignore) {
		}
		return builder.toString();
	}

	/* --- */

	/**
	 * factory method to get the AttributeTypeDescriptor for a particular field type
	 */
	public static AttributeTypeDescriptor resolve(CtClass managedCtClass, CtField persistentField) throws NotFoundException {
		boolean inherited = !managedCtClass.equals( persistentField.getDeclaringClass() );
		boolean visible = persistentField.visibleFrom( managedCtClass );
		String readerName = EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + persistentField.getName();
		String writerName = EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + persistentField.getName();
		InheritanceMetadata inheritanceMetadata = new InheritanceMetadata( inherited, visible, readerName, writerName );

		if ( CtClass.booleanType.equals( persistentField.getType() ) ) {
			return new PrimitiveAttributeTypeDescriptor( inheritanceMetadata, Boolean.TYPE );
		}
		else if ( CtClass.byteType.equals( persistentField.getType() )) {
			return new PrimitiveAttributeTypeDescriptor( inheritanceMetadata, Byte.TYPE );
		}
		else if ( CtClass.charType.equals( persistentField.getType() ) ) {
			return new PrimitiveAttributeTypeDescriptor( inheritanceMetadata, Character.TYPE );
		}
		else if ( CtClass.shortType.equals( persistentField.getType() ) ) {
			return new PrimitiveAttributeTypeDescriptor( inheritanceMetadata, Short.TYPE );
		}
		else if ( CtClass.intType.equals( persistentField.getType() ) ) {
			return new PrimitiveAttributeTypeDescriptor( inheritanceMetadata, Integer.TYPE );
		}
		else if ( CtClass.longType.equals( persistentField.getType() ) ) {
			return new PrimitiveAttributeTypeDescriptor( inheritanceMetadata, Long.TYPE );
		}
		else if ( CtClass.doubleType.equals( persistentField.getType() ) ) {
			return new PrimitiveAttributeTypeDescriptor( inheritanceMetadata, Double.TYPE );
		}
		else if ( CtClass.floatType.equals( persistentField.getType() ) ) {
			return new PrimitiveAttributeTypeDescriptor( inheritanceMetadata, Float.TYPE );
		}
		else {
			return new ObjectAttributeTypeDescriptor( inheritanceMetadata, persistentField.getType() );
		}
	}

	/* --- */

	/**
	 * AttributeTypeDescriptor for non primitive types
	 */
	private static class ObjectAttributeTypeDescriptor extends AttributeTypeDescriptor {

		private final String type;

		private ObjectAttributeTypeDescriptor(InheritanceMetadata inheritanceMetadata, CtClass concreteType) {
			super( inheritanceMetadata );
			this.type = concreteType.getName();
		}

		@Override
		public String buildReadInterceptionBodyFragment(String fieldName) {
			if ( inheritanceMetadata.isInherited() && !inheritanceMetadata.isVisible() ) {
				return String.format(
						" if( %3$s() != null ) { super.%5$s( (%2$s) %3$s().readObject(this, \"%1$s\", super.%4$s())); }%n",
						fieldName,
						type,
						EnhancerConstants.INTERCEPTOR_GETTER_NAME,
						inheritanceMetadata.getReaderName(),
						inheritanceMetadata.getWriterName() );
			}
			else {
				return String.format(
						"  if ( %3$s() != null ) { this.%1$s = (%2$s) %3$s().readObject(this, \"%1$s\", this.%1$s); }%n",
						fieldName,
						type,
						EnhancerConstants.INTERCEPTOR_GETTER_NAME );
			}
		}

		@Override
		public String buildWriteInterceptionBodyFragment(String fieldName) {
			if ( inheritanceMetadata.isInherited() && !inheritanceMetadata.isVisible() ) {
				return String.format(
						"  %2$s localVar = $1;%n" +
						"  if ( %3$s() != null ) { localVar = (%2$s) %3$s().writeObject(this, \"%1$s\", super.%4$s(), $1); }%n" +
						"  super.%5$s(localVar);",
						fieldName,
						type,
						EnhancerConstants.INTERCEPTOR_GETTER_NAME,
						inheritanceMetadata.getReaderName(),
						inheritanceMetadata.getWriterName() );
			}
			else {
				return String.format(
						"  %2$s localVar = $1;%n" +
						"  if ( %3$s() != null ) { localVar = (%2$s) %3$s().writeObject(this, \"%1$s\", this.%1$s, $1); }%n" +
						"  this.%1$s = localVar;",
						fieldName,
						type,
						EnhancerConstants.INTERCEPTOR_GETTER_NAME );
			}
		}
	}

	/**
	 * AttributeTypeDescriptor for primitive types
	 */
	private static class PrimitiveAttributeTypeDescriptor extends AttributeTypeDescriptor {

		private final String type;

		private PrimitiveAttributeTypeDescriptor(InheritanceMetadata inheritanceMetadata, Class<?> primitiveType) {
			super( inheritanceMetadata );
			if ( !primitiveType.isPrimitive() ) {
				throw new IllegalArgumentException( "Primitive attribute type descriptor can only be used on primitive types" );
			}
			// capitalize first letter
			this.type = primitiveType.getSimpleName().substring( 0, 1 ).toUpperCase( Locale.ROOT ) + primitiveType.getSimpleName().substring( 1 );
		}

		@Override
		public String buildReadInterceptionBodyFragment(String fieldName) {
			if ( inheritanceMetadata.isInherited() && !inheritanceMetadata.isVisible() ) {
				return String.format(
						"  if (%3$s() != null ) { super.%5$s( %3$s().read%2$s(this, \"%1$s\", super.%4$s())); }",
						fieldName,
						type,
						EnhancerConstants.INTERCEPTOR_GETTER_NAME,
						inheritanceMetadata.getReaderName(),
						inheritanceMetadata.getWriterName() );
			}
			else {
				return String.format(
						"  if (%3$s() != null ) { this.%1$s = %3$s().read%2$s(this, \"%1$s\", this.%1$s); }",
						fieldName,
						type,
						EnhancerConstants.INTERCEPTOR_GETTER_NAME );
			}
		}

		@Override
		public String buildWriteInterceptionBodyFragment(String fieldName) {
			if ( inheritanceMetadata.isInherited() && !inheritanceMetadata.isVisible() ) {
				return String.format(
						"  %2$s localVar = $1;%n" +
						"  if ( %4$s() != null ) { localVar = %4$s().write%3$s(this, \"%1$s\", super.%5$s(), $1); }%n" +
						"  super.%6$s(localVar);",
						fieldName,
						type.toLowerCase( Locale.ROOT ),
						type,
						EnhancerConstants.INTERCEPTOR_GETTER_NAME,
						inheritanceMetadata.getReaderName(),
						inheritanceMetadata.getWriterName() );
			}
			else {
				return String.format(
						"  %2$s localVar = $1;%n" +
						"  if ( %4$s() != null ) { localVar = %4$s().write%3$s(this, \"%1$s\", this.%1$s, $1); }%n" +
						"  this.%1$s = localVar;",
						fieldName,
						type.toLowerCase( Locale.ROOT ),
						type,
						EnhancerConstants.INTERCEPTOR_GETTER_NAME
				);
			}
		}
	}

	//

	private static class InheritanceMetadata {

		private boolean inherited;
		private boolean visible;
		private String readerName;
		private String writerName;

		public InheritanceMetadata(boolean inherited, boolean visible, String readerName, String writerName) {
			this.inherited = inherited;
			this.visible = visible;
			this.readerName = readerName;
			this.writerName = writerName;
		}

		public boolean isInherited() {
			return inherited;
		}

		public boolean isVisible() {
			return visible;
		}

		public String getReaderName() {
			return readerName;
		}

		public String getWriterName() {
			return writerName;
		}
	}

}
