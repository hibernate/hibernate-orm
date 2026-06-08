/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor;

import org.hibernate.processor.annotation.AnnotationMetaEntity;
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
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.hibernate.processor.util.Constants.ENTITY_LISTENER;
import static org.hibernate.processor.util.Constants.JD_STATIC_METAMODEL;
import static org.hibernate.processor.util.Constants.SPRING_COMPONENT;
import static org.hibernate.processor.util.Constants.STATIC_METAMODEL;
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
			var metaModelPackage = entity.getPackageName();
			// need to generate the body first, since this will also update
			// the required imports which need to be written out first
			var body = generateBody( entity, context ).toString();

			var fo = context.getProcessingEnvironment().getFiler().createSourceFile(
					getFullyQualifiedClassName( entity ),
					entity.getElement()
			);
			var os = fo.openOutputStream();
			var pw = new PrintWriter( os );

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
		final var sw = new StringWriter();
		try ( PrintWriter pw = new PrintWriter(sw) ) {

			pw.println( entity.javadoc() );

			if ( context.addComponentAnnotation() && entity.isInjectable() ) {
				pw.println( writeComponentAnnotation( entity ) );
			}
			if ( context.addDependentAnnotation() && entity.isInjectable() ) {
				pw.println( writeScopeAnnotation( entity ) );
			}
			if ( isLifecycleEventListener( entity ) ) {
				pw.println( writeEntityListenerAnnotation( entity ) );
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
						printAnnotation( entity, annotation, pw );
						pw.print('\n');
					} );

			printClassDeclaration( entity, pw );

			pw.println();

			final List<MetaAttribute> members = entity.getMembers();
			for ( var metaMember : members ) {
				if ( metaMember instanceof InnerClassMetaAttribute innerClass ) {
					generateBody( innerClass.getMetaEntity(), context )
							.toString().lines()
							.forEach(line -> pw.println('\t' + line));
					context.markGenerated( innerClass.getMetaEntity() );
				}
			}

			for ( var metaMember : members ) {
				if ( metaMember.hasStringAttribute() ) {
					metaMember.getAttributeNameDeclarationString().lines()
							.forEach(line -> pw.println('\t' + line));
				}
			}

			pw.println();

			for ( var metaMember : members ) {
				if ( metaMember.hasTypedAttribute() ) {
					metaMember.getAttributeDeclarationString().lines()
							.forEach(line -> {
								pw.println('\t' + line);
								if ( line.trim().startsWith("@Override") ) {
									metaMember.inheritedAnnotations()
											.forEach(annotation -> {
												pw.print('\t');
												printAnnotation( entity, annotation, pw );
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

	private static void printAnnotation(Metamodel entity, AnnotationMirror annotation, PrintWriter pw) {
		pw.print('@');
		final var type = (TypeElement) annotation.getAnnotationType().asElement();
		pw.print( entity.importType( type.getQualifiedName().toString() ) );
		var elementValues = annotation.getElementValues();
		if (!elementValues.isEmpty()) {
			pw.print('(');
			var first = true;
			for (var entry : elementValues.entrySet()) {
				if (first) {
					first = false;
				}
				else {
					pw.print(',');
				}
				pw.print( entry.getKey().getSimpleName() );
				pw.print( '=' );
				printAnnotationValue( entity, pw, entry.getValue() );
			}
			pw.print(')');
		}
	}

	private static void printAnnotationValue(Metamodel entity, PrintWriter pw, AnnotationValue value) {
		final var argument = value.getValue();
		if (argument instanceof VariableElement variable) {
			final var enclosing = variable.getEnclosingElement();
			if ( enclosing instanceof TypeElement typeElement ) {
				pw.print( entity.importType( typeElement.getQualifiedName().toString() ) );
			}
			else {
				pw.print( enclosing );
			}
			pw.print('.');
			pw.print( variable.getSimpleName().toString() );
		}
		else if (argument instanceof AnnotationMirror childAnnotation) {
			printAnnotation( entity, childAnnotation, pw );
		}
		else if (argument instanceof TypeMirror typeMirror) {
			pw.print( classLiteralName( entity, typeMirror ) );
			pw.print(".class");
		}
		else if (argument instanceof List<?> list) {
			pw.print('{');
			var first = true;
			for (var listedValue : list) {
				if (first) {
					first = false;
				}
				else {
					pw.print(',');
				}
				printAnnotationValue( entity, pw, (AnnotationValue) listedValue );
			}
			pw.print('}');
		}
		else {
			pw.print( argument );
		}
	}

	private static String classLiteralName(Metamodel entity, TypeMirror typeMirror) {
		if ( typeMirror instanceof DeclaredType declaredType
				&& declaredType.asElement() instanceof TypeElement typeElement ) {
			return entity.importType( typeElement.getQualifiedName().toString() );
		}
		else if ( typeMirror instanceof ArrayType arrayType ) {
			return classLiteralName( entity, arrayType.getComponentType() ) + "[]";
		}
		else {
			return typeMirror.toString();
		}
	}

	private static void printClassDeclaration(Metamodel entity, PrintWriter pw) {
		if ( isMemberType( entity.getElement() ) ) {
			final var modifiers = entity.getElement().getModifiers();
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
		pw.print( "class " );
		pw.print( getGeneratedClassName(entity) );

		final var superTypeElement = entity.getSuperTypeElement();
		if ( superTypeElement != null ) {
			final var generatedSuperclassName =
					getGeneratedSuperclassName( superTypeElement, entity.isJakartaDataStyle() );
			pw.print( " " + inheritanceKeyword( entity, generatedSuperclassName ) + " "
					+ entity.importType( generatedSuperclassName ) );
		}
		if ( entity.isImplementation() ) {
			pw.print( entity.getElement().getKind() == ElementKind.CLASS ? " extends " : " implements " );
			pw.print( entity.importType( entity.getQualifiedName() ) );
		}

		pw.println( " {" );
	}

	private static String inheritanceKeyword(Metamodel entity, String generatedSuperclassName) {
		final var generatedSuperclass =
				entity.getContext().getTypeElementForFullyQualifiedName( generatedSuperclassName );
		return entity.isJakartaDataStyle()
				&& generatedSuperclass != null
				&& generatedSuperclass.getKind().isInterface()
				? "implements"
				: "extends";
	}

	public static String getFullyQualifiedClassName(Metamodel entity) {
		if ( entity instanceof AnnotationMetaEntity annotationMetaEntity ) {
			return annotationMetaEntity.getGeneratedClassFullyQualifiedName();
		}
		else {
			return entity.getElement() instanceof PackageElement packageElement
					? packageElement.getQualifiedName() + "." + getGeneratedClassName( entity )
					: getGeneratedClassFullyQualifiedName( (TypeElement) entity.getElement(),
							entity.getPackageName(), isPrefixed( entity ) );
		}
	}

	private static String getGeneratedClassName(Metamodel entity) {
		final var className = entity.getSimpleName();
		return isPrefixed( entity ) ? '_' + className : className + '_';
	}

	private static boolean isPrefixed(Metamodel entity) {
		return entity.isJakartaDataStyle() || entity.isImplementation();
	}

	private static String getGeneratedSuperclassName(Element superClassElement, boolean jakartaDataStyle) {
		return getGeneratedClassName( (TypeElement) superClassElement, jakartaDataStyle );
	}

	private static String getGeneratedClassName(TypeElement typeElement, boolean jakartaDataStyle) {
		final var simpleName = typeElement.getSimpleName().toString();
		final var enclosingElement = typeElement.getEnclosingElement();
		return (enclosingElement instanceof TypeElement
				? getGeneratedSuperclassName( enclosingElement, jakartaDataStyle )
				: ((PackageElement) enclosingElement).getQualifiedName().toString())
				+ "." + (jakartaDataStyle ? '_' + simpleName : simpleName + '_');
	}

	private static String writeGeneratedAnnotation(Metamodel entity, Context context) {
		var generatedAnnotation = new StringBuilder();
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
		final var annotation = new StringBuilder("@SuppressWarnings({");
		final var warnings = context.getSuppressedWarnings();
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

	private static boolean isLifecycleEventListener(Metamodel entity) {
		return entity instanceof AnnotationMetaEntity annotationMetaEntity
			&& annotationMetaEntity.isLifecycleEventListener();
	}

	private static String writeEntityListenerAnnotation(Metamodel entity) {
		return "@" + entity.importType( ENTITY_LISTENER );
	}

	private static String writeComponentAnnotation(Metamodel entity) {
		return "@" + entity.importType( SPRING_COMPONENT );
	}

	private static String writeStaticMetaModelAnnotation(Metamodel entity) {
		final var annotation = entity.isJakartaDataStyle()
				? JD_STATIC_METAMODEL
				: STATIC_METAMODEL;
		final var simpleName = entity.importType( entity.getQualifiedName() );
		return "@" + entity.importType( annotation ) + "(" + simpleName + ".class)";
	}
}
