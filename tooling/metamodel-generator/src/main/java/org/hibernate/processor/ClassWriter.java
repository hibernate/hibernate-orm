/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor;

import org.hibernate.processor.annotation.InnerClassMetaAttribute;
import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;

import javax.annotation.processing.FilerException;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
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
import java.util.Set;

import static org.hibernate.processor.util.Constants.SPRING_COMPONENT;
import static org.hibernate.processor.util.TypeUtils.getGeneratedClassFullyQualifiedName;
import static org.hibernate.processor.util.TypeUtils.isMemberType;

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
					getFullyQualifiedClassName( entity ),
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

			pw.println( entity.javadoc() );

			if ( context.addComponentAnnotation() && entity.isInjectable() ) {
				pw.println( writeComponentAnnotation( entity ) );
			}
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
				if ( metaMember instanceof InnerClassMetaAttribute innerClass ) {
					generateBody( innerClass.getMetaEntity(), context )
							.toString().lines()
							.forEach(line -> pw.println('\t' + line));
					context.markGenerated( innerClass.getMetaEntity() );
				}
			}

			for ( MetaAttribute metaMember : members ) {
				if ( metaMember.hasStringAttribute() ) {
					metaMember.getAttributeNameDeclarationString().lines()
							.forEach(line -> pw.println('\t' + line));
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
		if (argument instanceof VariableElement variable) {
			pw.print( variable.getEnclosingElement() );
			pw.print('.');
			pw.print( variable.getSimpleName().toString() );
		}
		else if (argument instanceof AnnotationMirror childAnnotation) {
			printAnnotation( childAnnotation, pw );
		}
		else if (argument instanceof TypeMirror) {
			pw.print(argument);
			pw.print(".class");
		}
		else if (argument instanceof List) {
			final var list = (List<? extends AnnotationValue>) argument;
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
		if ( isMemberType( entity.getElement() ) ) {
			final Set<Modifier> modifiers = entity.getElement().getModifiers();
			if ( modifiers.contains( Modifier.PUBLIC ) ) {
				pw.print( "public " );
			}
			else if ( modifiers.contains( Modifier.PROTECTED ) ) {
				pw.print( "protected " );
			}
			pw.print( "static " );
		}
		else {
			pw.print( "public " );
		}
		if ( !entity.isImplementation() && !entity.isJakartaDataStyle() ) {
			pw.print( "abstract " );
		}
		pw.print( entity.isJakartaDataStyle() ? "interface " : "class " );
		pw.print( getGeneratedClassName(entity) );

		final Element superTypeElement = entity.getSuperTypeElement();
		if ( superTypeElement != null ) {
			pw.print( " extends " +
					entity.importType(getGeneratedSuperclassName( superTypeElement, entity.isJakartaDataStyle() )) );
		}
		if ( entity.isImplementation() ) {
			pw.print( entity.getElement().getKind() == ElementKind.CLASS ? " extends " : " implements " );
			pw.print( entity.getSimpleName() );
		}

		pw.println( " {" );
	}

	private static String getFullyQualifiedClassName(Metamodel entity) {
		return entity.getElement() instanceof PackageElement packageElement
				? packageElement.getQualifiedName().toString() + "." + getGeneratedClassName( entity )
				: getGeneratedClassFullyQualifiedName( (TypeElement) entity.getElement(),
						entity.getPackageName(), entity.isJakartaDataStyle() );
	}

	private static String getGeneratedClassName(Metamodel entity) {
		final String className = entity.getSimpleName();
		return entity.isJakartaDataStyle() ? '_' + className : className + '_';
	}

	private static String getGeneratedSuperclassName(Element superClassElement, boolean jakartaDataStyle) {
		return getGeneratedClassName( (TypeElement) superClassElement, jakartaDataStyle );
	}

	private static String getGeneratedClassName(TypeElement typeElement, boolean jakartaDataStyle) {
		final String simpleName = typeElement.getSimpleName().toString();
		final Element enclosingElement = typeElement.getEnclosingElement();
		return (enclosingElement instanceof TypeElement
				? getGeneratedSuperclassName( enclosingElement, jakartaDataStyle )
				: ((PackageElement) enclosingElement).getQualifiedName().toString())
				+ "." + (jakartaDataStyle ? '_' + simpleName : simpleName + '_');
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

	private static String writeComponentAnnotation(Metamodel entity) {
		return "@" + entity.importType( SPRING_COMPONENT );
	}

	private static String writeStaticMetaModelAnnotation(Metamodel entity) {
		final String annotation = entity.isJakartaDataStyle()
				? "jakarta.data.metamodel.StaticMetamodel"
				: "jakarta.persistence.metamodel.StaticMetamodel";
		final String simpleName = entity.importType( entity.getQualifiedName() );
		return "@" + entity.importType( annotation ) + "(" + simpleName + ".class)";
	}
}
