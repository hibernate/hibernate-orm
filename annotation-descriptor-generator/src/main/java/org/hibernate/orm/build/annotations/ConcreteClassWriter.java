/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.build.annotations;

import java.io.IOException;
import java.io.Writer;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.JavaFileObject;

import org.hibernate.orm.build.annotations.structure.AnnotationDescriptor;
import org.hibernate.orm.build.annotations.structure.AttributeDescriptor;

import static org.hibernate.orm.build.annotations.ClassFileHelper.GENERATION_PACKAGE;

/**
 * Writes the concrete annotation class to file
 *
 * @author Steve Ebersole
 */
public class ConcreteClassWriter extends AbstractClassWriter {

	public static void writeClass(
			AnnotationDescriptor annotationDescriptor,
			ProcessingEnvironment processingEnv) {
		final String sourceFileName = GENERATION_PACKAGE + "." + annotationDescriptor.concreteTypeName();

		final Filer filer = processingEnv.getFiler();
		try {
			final JavaFileObject sourceFile = filer.createSourceFile( sourceFileName, annotationDescriptor.annotationType() );
			try (Writer writer = sourceFile.openWriter()) {
				final ConcreteClassWriter classWriter = new ConcreteClassWriter( annotationDescriptor, writer, processingEnv );
				classWriter.write();
			}
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to create concrete Annotation class source file : " + sourceFileName, e );
		}
	}

	private final AnnotationDescriptor annotationDescriptor;
	private final ProcessingEnvironment processingEnv;

	public ConcreteClassWriter(
			AnnotationDescriptor annotationDescriptor,
			Writer writer,
			ProcessingEnvironment processingEnv) {
		super( writer );
		this.annotationDescriptor = annotationDescriptor;
		this.processingEnv = processingEnv;
	}

	private void write() throws IOException {
		writeLine( "package %s;", GENERATION_PACKAGE );

		writer.write( '\n' );

		writeLine( "import java.lang.annotation.Annotation;" );
		writeLine();
		writeLine( "import org.hibernate.models.spi.SourceModelBuildingContext;" );
		writeLine();
		writeLine( "import org.jboss.jandex.AnnotationInstance;" );
		writeLine();
		writeLine( "import %s;", annotationDescriptor.annotationType().getQualifiedName().toString() );
		writeLine();
		writeLine( "import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;" );
		writeLine( "import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;" );

		writer.write( '\n' );

		writeLine( "@SuppressWarnings({ \"ClassExplicitlyAnnotation\", \"unused\" })" );
		writeLine( "@jakarta.annotation.Generated(\"org.hibernate.orm.build.annotations.ClassGeneratorProcessor\")" );
		writeLine( "public class %s implements %s {", annotationDescriptor.concreteTypeName(), annotationDescriptor.annotationType().getSimpleName().toString() );

		writeDescriptorConstant();
		writeFields();
		writeConstructors();
		writeMethods();

		writeLine( "}" );
	}

	private void writeDescriptorConstant() throws IOException {
//		writeLine(
//				1,
//				"public static final %s<%s,%> %s = new %s(%s.class, %s.class, %s);",
//				"OrmAnnotationDescriptor",
//				annotationDescriptor.annotationType().getSimpleName(),
//				annotationDescriptor.concreteTypeName(),
//				annotationDescriptor.constantName(),
//				"OrmAnnotationDescriptor",
//				annotationDescriptor.annotationType().getSimpleName(),
//				annotationDescriptor.concreteTypeName(),
//				annotationDescriptor.repeatableContainerConstantName()
//		);
		writeLine(
				1,
				"public static final %s<%s,%s> %s = null;",
				"OrmAnnotationDescriptor",
				annotationDescriptor.annotationType().getSimpleName(),
				annotationDescriptor.concreteTypeName(),
				annotationDescriptor.constantName()
		);
		writeLine();
	}

	private void writeFields() throws IOException {
		for ( AttributeDescriptor attribute : annotationDescriptor.attributes() ) {
			writeLine( 1, "private %s %s;", attribute.getType().getTypeDeclarationString(), attribute.getName() );
		}
		writeLine();
	}

	private void writeConstructors() throws IOException {
		writeDefaultInitialization();
		writeJdkInitialization();
		writeJandexInitialization();
	}

	private void writeDefaultInitialization() throws IOException {
		writeLine( 1, "/**" );
		writeLine( 1, " * Used in creating dynamic annotation instances (e.g. from XML)" );
		writeLine( 1, " */" );
		writeLine( 1, "public %s(SourceModelBuildingContext modelContext) {", annotationDescriptor.concreteTypeName() );

		for ( AttributeDescriptor attribute : annotationDescriptor.attributes() ) {
			writeDefaultValueInitialization( attribute );
		}

		writeLine( 1, "}" );
		writeLine();
	}

	private void writeDefaultValueInitialization(AttributeDescriptor attribute) throws IOException {
		if ( attribute.getDefaultValue() == null || attribute.getDefaultValue().getValue() == null ) {
			return;
		}

		writeLine( 2, "this.%s = %s;", attribute.getName(),attribute.getType().getInitializerValue( attribute.getDefaultValue() ) );
	}

	private void writeJdkInitialization() throws IOException {
		writeLine( 1, "/**" );
		writeLine( 1, " * Used in creating annotation instances from JDK variant" );
		writeLine( 1, " */" );
		writeLine( 1, "public %s(%s annotation, SourceModelBuildingContext modelContext) {", annotationDescriptor.concreteTypeName(), annotationDescriptor.annotationType().getSimpleName().toString() );

		for ( AttributeDescriptor attributeDescriptor : annotationDescriptor.attributes() ) {
			writeJdkValueInitialization( attributeDescriptor );
		}

		writeLine( 1, "}" );
		writeLine();
	}

	private void writeJdkValueInitialization(AttributeDescriptor attribute) throws IOException {
		writeLine(
				2,
				"this.%s = extractJdkValue( annotation, %s, \"%s\", modelContext );",
				attribute.getName(),
				annotationDescriptor.getConstantFqn(),
				attribute.getName()
		);
	}

	private void writeJandexInitialization() throws IOException {
		writeLine( 1, "/**" );
		writeLine( 1, " * Used in creating annotation instances from Jandex variant" );
		writeLine( 1, " */" );
		writeLine( 1, "public %s(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {", annotationDescriptor.concreteTypeName() );

		for ( AttributeDescriptor attributeDescriptor : annotationDescriptor.attributes() ) {
			writeJandexValueInitialization( attributeDescriptor );
		}

		writeLine( 1, "}" );
		writeLine();
	}

	private void writeJandexValueInitialization(AttributeDescriptor attributeDescriptor) throws IOException {
		final String attrName = attributeDescriptor.getName();

		writeLine(
				2,
				"this.%s = extractJandexValue( annotation, %s, \"%s\", modelContext );",
				attrName,
				annotationDescriptor.getConstantFqn(),
				attrName
		);
	}

	private void writeMethods() throws IOException {
		writeAnnotationTypeMethod();
		writeGettersAndSetters();
		writeHelperMethods();
	}

	private void writeAnnotationTypeMethod() throws IOException {
		writeLine( 1, "@Override" );
		writeLine( 1, "public Class<? extends Annotation> annotationType() {" );
		writeLine( 2, "return %s.class;", annotationDescriptor.annotationType().getSimpleName() );
		writeLine( 1, "}" );
		writeLine();
	}

	private void writeGettersAndSetters() throws IOException {
		for ( AttributeDescriptor attribute : annotationDescriptor.attributes() ) {
			writeGetterAndSetter( attribute );
			writeLine();
		}
	}

	private void writeGetterAndSetter(AttributeDescriptor attribute) throws IOException {
		// "getter"
		writeLine(
				1,
				"@Override public %s %s() { return %s; }",
				attribute.getType().getTypeDeclarationString(),
				attribute.getName(),
				attribute.getName()
		);

		// "setter"
		writeLine(
				1,
				"public void %s(%s value) { this.%s = value; }",
				attribute.getName(),
				attribute.getType().getTypeDeclarationString(),
				attribute.getName()
		);

		writeLine();
	}

	private void writeHelperMethods() throws IOException {
//		writeLine( 1, "public static <V, A extends Annotation> V extractJdkValue(A jdkAnnotation, AttributeDescriptor<V> attributeDescriptor, SourceModelBuildingContext modelContext) {" );
//		writeLine( 2, "return attributeDescriptor.getTypeDescriptor().createJdkValueExtractor( modelContext ).extractValue( jdkAnnotation, attributeDescriptor, modelContext );" );
//		writeLine( 1, "}" );
//		writeLine();
//		writeLine( 1, "public static <V, A extends Annotation> V extractJdkValue(A jdkAnnotation, AnnotationDescriptor<A> annotationDescriptor, String attributeName, SourceModelBuildingContext modelContext) {" );
//		writeLine( 2, "final AttributeDescriptor<V> attributeDescriptor = annotationDescriptor.getAttribute( attributeName );" );
//		writeLine( 2, "return extractJdkValue( jdkAnnotation, attributeDescriptor, modelContext );" );
//		writeLine( 1, "}" );
//		writeLine();
//		writeLine( 1, "public static <V> V extractJandexValue(AnnotationInstance jandexAnnotation, AttributeDescriptor<V> attributeDescriptor, SourceModelBuildingContext modelContext) {" );
//		writeLine( 2, "final AnnotationValue value = jandexAnnotation.value( attributeDescriptor.getName() );" );
//		writeLine( 2, "return attributeDescriptor.getTypeDescriptor().createJandexValueConverter( modelContext ).convert( value, modelContext );" );
//		writeLine( 1, "}" );
//		writeLine();
//		writeLine( 1, "public static <V, A extends Annotation> V extractJandexValue(AnnotationInstance jandexAnnotation, AnnotationDescriptor<A> annotationDescriptor, String attributeName, SourceModelBuildingContext modelContext) {" );
//		writeLine( 2, "final AttributeDescriptor<V> attributeDescriptor = %s.getAttribute( attributeName );" );
//		writeLine( 2, "return extractJandexValue( jandexAnnotation, attributeDescriptor, modelContext );" );
//		writeLine( 1, "}" );
	}

}
