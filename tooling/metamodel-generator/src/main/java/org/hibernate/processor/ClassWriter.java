/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor;

import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;

import javax.annotation.processing.FilerException;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Helper class to write the actual metamodel class using the  {@link javax.annotation.processing.Filer} API.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class ClassWriter {

	private ClassWriter() {
	}

	public static void writeFile(Metamodel entity, Context context) {
		try {
			String metaModelPackage = entity.getPackageName();
			// need to generate the body first, since this will also update
			// the required imports which need to be written out first
			String body = generateBody( entity, context ).toString();

			FileObject fo = context.getProcessingEnvironment().getFiler().createSourceFile(
					getFullyQualifiedClassName( entity, metaModelPackage ),
					entity.getElement()
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
					"Problem opening file to write MetaModel for " + entity.getSimpleName() + ": " + ioEx.getMessage()
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
	private static StringBuffer generateBody(Metamodel entity, Context context) {
		final StringWriter sw = new StringWriter();
		try ( PrintWriter pw = new PrintWriter(sw) ) {

			if ( context.addDependentAnnotation() && entity.isInjectable() ) {
				pw.println( writeScopeAnnotation( entity ) );
			}
			if ( entity.getElement() instanceof TypeElement && !entity.isInjectable() ) {
				pw.println( writeStaticMetaModelAnnotation( entity ) );
			}
			if ( context.addGeneratedAnnotation() ) {
				pw.println( writeGeneratedAnnotation( entity, context ) );
			}
			if ( context.addSuppressWarningsAnnotation() ) {
				pw.println( writeSuppressWarnings(context) );
			}
			entity.inheritedAnnotations()
					.forEach( annotation -> {
						printAnnotation( annotation, pw );
						pw.print('\n');
					} );

			printClassDeclaration( entity, pw );

			pw.println();

			final List<MetaAttribute> members = entity.getMembers();
			for ( MetaAttribute metaMember : members ) {
				if ( metaMember.hasStringAttribute() ) {
					pw.println( '\t' + metaMember.getAttributeNameDeclarationString() );
				}
			}

			pw.println();

			for ( MetaAttribute metaMember : members ) {
				if ( metaMember.hasTypedAttribute() ) {
					metaMember.getAttributeDeclarationString().lines()
							.forEach(line -> {
								pw.println('\t' + line);
								if ( line.trim().startsWith("@Override") ) {
									metaMember.inheritedAnnotations()
											.forEach(annotation -> {
												pw.print('\t');
												printAnnotation( annotation, pw );
												pw.print('\n');
											});
								}
							});
				}
			}

			pw.println();
			pw.println("}");
			return sw.getBuffer();
		}
	}

	private static void printAnnotation(AnnotationMirror annotation, PrintWriter pw) {
		pw.print('@');
		final TypeElement type = (TypeElement) annotation.getAnnotationType().asElement();
		pw.print( type.getQualifiedName().toString() );
		var elementValues = annotation.getElementValues();
		if (!elementValues.isEmpty()) {
			pw.print('(');
			boolean first = true;
			for (var entry : elementValues.entrySet()) {
				if (first) {
					first = false;
				}
				else {
					pw.print(',');
				}
				pw.print( entry.getKey().getSimpleName() );
				pw.print( '=' );
				printAnnotationValue( pw, entry.getValue() );
			}
			pw.print(')');
		}
	}

	private static void printAnnotationValue(PrintWriter pw, AnnotationValue value) {
		final Object argument = value.getValue();
		if (argument instanceof VariableElement) {
			VariableElement variable = (VariableElement) argument;
			pw.print( variable.getEnclosingElement() );
			pw.print('.');
			pw.print( variable.getSimpleName().toString() );
		}
		else if (argument instanceof AnnotationMirror) {
			AnnotationMirror childAnnotation = (AnnotationMirror) argument;
			printAnnotation( childAnnotation, pw );
		}
		else if (argument instanceof TypeMirror) {
			pw.print(argument);
			pw.print(".class");
		}
		else if (argument instanceof List) {
			final List<? extends AnnotationValue> list =
					(List<? extends AnnotationValue>) argument;
			pw.print('{');
			boolean first = true;
			for (AnnotationValue listedValue : list) {
				if (first) {
					first = false;
				}
				else {
					pw.print(',');
				}
				printAnnotationValue( pw, listedValue );
			}
			pw.print('}');
		}
		else {
			pw.print( argument );
		}
	}

	private static void printClassDeclaration(Metamodel entity, PrintWriter pw) {
		pw.print( "public " );
		if ( !entity.isImplementation() && !entity.isJakartaDataStyle() ) {
			pw.print( "abstract " );
		}
		pw.print( entity.isJakartaDataStyle() ? "interface " : "class " );
		pw.print( getGeneratedClassName(entity) );

		String superClassName = entity.getSupertypeName();
		if ( superClassName != null ) {
			pw.print( " extends " + getGeneratedSuperclassName(entity, superClassName) );
		}
		if ( entity.isImplementation() ) {
			pw.print( entity.getElement().getKind() == ElementKind.CLASS ? " extends " : " implements " );
			pw.print( entity.getSimpleName() );
		}

		pw.println( " {" );
	}

	private static String getFullyQualifiedClassName(Metamodel entity, String metaModelPackage) {
		String fullyQualifiedClassName = "";
		if ( !metaModelPackage.isEmpty() ) {
			fullyQualifiedClassName = fullyQualifiedClassName + metaModelPackage + ".";
		}
		fullyQualifiedClassName = fullyQualifiedClassName + getGeneratedClassName( entity );
		return fullyQualifiedClassName;
	}

	private static String getGeneratedClassName(Metamodel entity) {
		final String className = entity.getSimpleName();
		return entity.isJakartaDataStyle() ? '_' + className : className + '_';
	}

	private static String getGeneratedSuperclassName(Metamodel entity, String superClassName) {
		if ( entity.isJakartaDataStyle() ) {
			int lastDot = superClassName.lastIndexOf('.');
			if ( lastDot<0 ) {
				return '_' + superClassName;
			}
			else {
				return superClassName.substring(0,lastDot+1)
						+ '_' + superClassName.substring(lastDot+1);
			}
		}
		else {
			return superClassName + '_';
		}
	}

	private static String writeGeneratedAnnotation(Metamodel entity, Context context) {
		StringBuilder generatedAnnotation = new StringBuilder();
		generatedAnnotation
				.append( "@" )
				.append( entity.importType( "jakarta.annotation.Generated" ) )
				.append( "(" );
		if ( context.addGeneratedDate() ) {
			generatedAnnotation
					.append( "value = " );
		}
		generatedAnnotation
				.append( "\"" )
				.append( HibernateProcessor.class.getName() )
				.append( "\"" );
		if ( context.addGeneratedDate() ) {
			generatedAnnotation
					.append( ", date = " )
					.append( "\"" )
					.append( DateTimeFormatter.ISO_OFFSET_DATE_TIME.format( OffsetDateTime.now() ) )
					.append( "\"" );
		}
		generatedAnnotation.append( ")" );
		return generatedAnnotation.toString();
	}

	private static String writeSuppressWarnings(Context context) {
		final StringBuilder annotation = new StringBuilder("@SuppressWarnings({");
		final String[] warnings = context.getSuppressedWarnings();
		for (int i = 0; i < warnings.length; i++) {
			if ( i>0 ) {
				annotation.append(", ");
			}
			annotation.append('"').append(warnings[i]).append('"');
		}
		return annotation.append("})").toString();
	}

	private static String writeScopeAnnotation(Metamodel entity) {
		return "@" + entity.importType( entity.scope() );
	}

	private static String writeStaticMetaModelAnnotation(Metamodel entity) {
		final String annotation = entity.isJakartaDataStyle()
				? "jakarta.data.metamodel.StaticMetamodel"
				: "jakarta.persistence.metamodel.StaticMetamodel";
		return "@" + entity.importType( annotation ) + "(" + entity.getSimpleName() + ".class)";
	}
}
