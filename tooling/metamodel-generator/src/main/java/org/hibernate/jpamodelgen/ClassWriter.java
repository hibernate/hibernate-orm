/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.annotation.processing.FilerException;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;

import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.MetaEntity;
import org.hibernate.jpamodelgen.util.Constants;
import org.hibernate.jpamodelgen.util.TypeUtils;

/**
 * Helper class to write the actual meta model class using the  {@link javax.annotation.processing.Filer} API.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class ClassWriter {
	private static final String META_MODEL_CLASS_NAME_SUFFIX = "_";
	private static final ThreadLocal<SimpleDateFormat> SIMPLE_DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
		@Override
		public SimpleDateFormat initialValue() {
			return new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );
		}
	};

	private ClassWriter() {
	}

	public static void writeFile(MetaEntity entity, Context context) {
		try {
			String metaModelPackage = entity.getPackageName();
			// need to generate the body first, since this will also update the required imports which need to
			// be written out first
			String body = generateBody( entity, context ).toString();

			FileObject fo = context.getProcessingEnvironment().getFiler().createSourceFile(
					getFullyQualifiedClassName( entity, metaModelPackage ),
					entity.getTypeElement()
			);
			OutputStream os = fo.openOutputStream();
			PrintWriter pw = new PrintWriter( os );

			if ( !metaModelPackage.isEmpty() ) {
				pw.println( "package " + metaModelPackage + ";" );
				pw.println();
			}
			pw.println( entity.generateImports() );
			pw.println( body );

			pw.flush();
			pw.close();
		}
		catch ( FilerException filerEx ) {
			context.logMessage(
					Diagnostic.Kind.ERROR, "Problem with Filer: " + filerEx.getMessage()
			);
		}
		catch ( IOException ioEx ) {
			context.logMessage(
					Diagnostic.Kind.ERROR,
					"Problem opening file to write MetaModel for " + entity.getSimpleName() + ioEx.getMessage()
			);
		}
	}

	/**
	 * Generate everything after import statements.
	 *
	 * @param entity The meta entity for which to write the body
	 * @param context The processing context
	 *
	 * @return body content
	 */
	private static StringBuffer generateBody(MetaEntity entity, Context context) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = null;
		try {
			pw = new PrintWriter( sw );

			if ( context.addGeneratedAnnotation() ) {
				pw.println( writeGeneratedAnnotation( entity, context ) );
			}
			if ( context.isAddSuppressWarningsAnnotation() ) {
				pw.println( writeSuppressWarnings() );
			}

			pw.println( writeStaticMetaModelAnnotation( entity ) );

			printClassDeclaration( entity, pw, context );

			pw.println();

			List<MetaAttribute> members = entity.getMembers();
			for ( MetaAttribute metaMember : members ) {
				pw.println( "	" + metaMember.getAttributeDeclarationString() );
			}
			pw.println();
			for ( MetaAttribute metaMember : members ) {
				pw.println( "	" + metaMember.getAttributeNameDeclarationString() );
			}

			pw.println();
			pw.println( "}" );
			return sw.getBuffer();
		}
		finally {
			if ( pw != null ) {
				pw.close();
			}
		}
	}

	private static void printClassDeclaration(MetaEntity entity, PrintWriter pw, Context context) {
		pw.print( "public abstract class " + entity.getSimpleName() + META_MODEL_CLASS_NAME_SUFFIX );

		String superClassName = findMappedSuperClass( entity, context );
		if ( superClassName != null ) {
			pw.print( " extends " + superClassName + META_MODEL_CLASS_NAME_SUFFIX );
		}

		pw.println( " {" );
	}

	private static String findMappedSuperClass(MetaEntity entity, Context context) {
		TypeMirror superClass = entity.getTypeElement().getSuperclass();
		//superclass of Object is of NoType which returns some other kind
		while ( superClass.getKind() == TypeKind.DECLARED ) {
			//F..king Ch...t Have those people used their horrible APIs even once?
			final Element superClassElement = ( (DeclaredType) superClass ).asElement();
			String superClassName = ( (TypeElement) superClassElement ).getQualifiedName().toString();
			if ( extendsSuperMetaModel( superClassElement, entity.isMetaComplete(), context ) ) {
				return superClassName;
			}
			superClass = ( (TypeElement) superClassElement ).getSuperclass();
		}
		return null;
	}

	/**
	 * Checks whether this metamodel class needs to extend another metamodel class.
	 * This methods checks whether the processor has generated a metamodel class for the super class, but it also
	 * allows for the possibility that the metamodel class was generated in a previous compilation (eg it could be
	 * part of a separate jar. See also METAGEN-35).
	 *
	 * @param superClassElement the super class element
	 * @param entityMetaComplete flag indicating if the entity for which the metamodel should be generarted is metamodel
	 * complete. If so we cannot use reflection to decide whether we have to add the extend clause
	 * @param context the execution context
	 *
	 * @return {@code true} in case there is super class meta model to extend from {@code false} otherwise.
	 */
	private static boolean extendsSuperMetaModel(Element superClassElement, boolean entityMetaComplete, Context context) {
		// if we processed the superclass in the same run we definitely need to extend
		String superClassName = ( (TypeElement) superClassElement ).getQualifiedName().toString();
		if ( context.containsMetaEntity( superClassName )
				|| context.containsMetaEmbeddable( superClassName ) ) {
			return true;
		}

		// to allow for the case that the metamodel class for the super entity is for example contained in another
		// jar file we use reflection. However, we need to consider the fact that there is xml configuration
		// and annotations should be ignored
		if ( !entityMetaComplete && ( TypeUtils.containsAnnotation( superClassElement, Constants.ENTITY )
				|| TypeUtils.containsAnnotation( superClassElement, Constants.MAPPED_SUPERCLASS ) ) ) {
			return true;
		}

		return false;
	}

	private static String getFullyQualifiedClassName(MetaEntity entity, String metaModelPackage) {
		String fullyQualifiedClassName = "";
		if ( !metaModelPackage.isEmpty() ) {
			fullyQualifiedClassName = fullyQualifiedClassName + metaModelPackage + ".";
		}
		fullyQualifiedClassName = fullyQualifiedClassName + entity.getSimpleName() + META_MODEL_CLASS_NAME_SUFFIX;
		return fullyQualifiedClassName;
	}

	private static String writeGeneratedAnnotation(MetaEntity entity, Context context) {
		StringBuilder generatedAnnotation = new StringBuilder();
		generatedAnnotation.append( "@" )
				.append( entity.importType( context.getGeneratedAnnotation().getQualifiedName().toString() ) )
				.append( "(value = \"" )
				.append( JPAMetaModelEntityProcessor.class.getName() );
		if ( context.addGeneratedDate() ) {
			generatedAnnotation.append( "\", date = \"" )
					.append( SIMPLE_DATE_FORMAT.get().format( new Date() ) )
					.append( "\")" );
		}
		else {
			generatedAnnotation.append( "\")" );
		}
		return generatedAnnotation.toString();
	}

	private static String writeSuppressWarnings() {
		return "@SuppressWarnings({ \"deprecation\", \"rawtypes\" })";
	}

	private static String writeStaticMetaModelAnnotation(MetaEntity entity) {
		return "@" + entity.importType( "javax.persistence.metamodel.StaticMetamodel" ) + "(" + entity.getSimpleName() + ".class)";
	}
}
