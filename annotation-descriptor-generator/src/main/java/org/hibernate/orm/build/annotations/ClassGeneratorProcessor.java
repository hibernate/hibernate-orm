/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.build.annotations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;

import org.hibernate.orm.build.annotations.structure.AnnotationDescriptor;
import org.hibernate.orm.build.annotations.structure.AnnotationType;
import org.hibernate.orm.build.annotations.structure.AttributeDescriptor;
import org.hibernate.orm.build.annotations.structure.BooleanType;
import org.hibernate.orm.build.annotations.structure.EnumType;
import org.hibernate.orm.build.annotations.structure.IntType;
import org.hibernate.orm.build.annotations.structure.LongType;
import org.hibernate.orm.build.annotations.structure.ShortType;
import org.hibernate.orm.build.annotations.structure.Type;

import static org.hibernate.orm.build.annotations.structure.StringType.STRING_TYPE;

/**
 * @author Steve Ebersole
 */
@SupportedAnnotationTypes( "java.lang.annotation.Retention" )
public class ClassGeneratorProcessor extends AbstractProcessor {
	public static final String JPA_PACKAGE = "jakarta.persistence";
	public static final String HIBERNATE_PACKAGE = "org.hibernate.annotations";
	public static final String HIBERNATE_PACKAGE2 = "org.hibernate.boot.internal";
	public static final String DIALECT_OVERRIDES = "org.hibernate.annotations.DialectOverride";

	private final Map<TypeElement, AnnotationDescriptor> annotationDescriptorMap = new TreeMap<>( Comparator.comparing( typeElement -> typeElement.getSimpleName().toString() ) );

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if ( roundEnv.processingOver() ) {
			finishUp();
		}
		else {
			processAnnotations( roundEnv );
		}

		return false;
	}

	private void processAnnotations(RoundEnvironment roundEnv) {
		processAnnotations(
				processingEnv.getElementUtils().getPackageElement( JPA_PACKAGE ),
				"%sJpaAnnotation",
				"org.hibernate.boot.models.JpaAnnotations"
		);

		processAnnotations(
				processingEnv.getElementUtils().getPackageElement( HIBERNATE_PACKAGE ),
				"%sAnnotation",
				"org.hibernate.boot.models.HibernateAnnotations"
		);

		processAnnotations(
				processingEnv.getElementUtils().getPackageElement( HIBERNATE_PACKAGE2 ),
				"%sXmlAnnotation",
				"org.hibernate.boot.models.XmlAnnotations"
		);

		processAnnotations(
				processingEnv.getElementUtils().getTypeElement( DIALECT_OVERRIDES ),
				"%sAnnotation",
				"org.hibernate.boot.models.DialectOverrideAnnotations"
		);
	}

	private void processAnnotations(PackageElement packageElement, String concreteNamePattern, String constantsClassName) {
		for ( Element enclosedElement : packageElement.getEnclosedElements() ) {
			if ( enclosedElement instanceof TypeElement typeElement ) {
				if ( typeElement.getKind() == ElementKind.ANNOTATION_TYPE ) {
					processAnnotation( typeElement, concreteNamePattern, constantsClassName, annotationDescriptorMap );
				}
			}
		}
	}

	private void processAnnotations(TypeElement typeElement, String concreteNamePattern, String constantsClassName) {
		for ( Element enclosedElement : typeElement.getEnclosedElements() ) {
			if ( enclosedElement instanceof TypeElement nestedTypeElement ) {
				if ( nestedTypeElement.getKind() == ElementKind.ANNOTATION_TYPE ) {
					processAnnotation( nestedTypeElement, concreteNamePattern, constantsClassName, annotationDescriptorMap );
				}
			}
		}
	}

	private void processAnnotation(
			TypeElement annotationClass,
			String concreteNamePattern,
			String constantsClassName,
			Map<TypeElement, AnnotationDescriptor> descriptorMap) {
		if ( descriptorMap.containsKey( annotationClass ) ) {
			// we've already processed it
			return;
		}

		final String concreteClassName = String.format( concreteNamePattern, annotationClass.getSimpleName().toString() );
		final String constantName = ClassFileHelper.determineConstantName( annotationClass );
		final String repeatableContainerConstantName = resolveRepeatableContainer( annotationClass );

		final AnnotationDescriptor annotationDescriptor = new AnnotationDescriptor(
				annotationClass,
				concreteClassName,
				constantsClassName,
				constantName,
				repeatableContainerConstantName,
				extractAttributes( annotationClass )
		);
		descriptorMap.put( annotationClass, annotationDescriptor );

		ConcreteClassWriter.writeClass( annotationDescriptor, processingEnv );
	}

	private List<AttributeDescriptor> extractAttributes(TypeElement annotationType) {
		final List<? extends Element> allMembers = processingEnv.getElementUtils().getAllMembers( annotationType );
		final List<AttributeDescriptor> attributeDescriptors = new ArrayList<>( allMembers.size() );

		for ( Element member : allMembers ) {
			if ( member.getKind() != ElementKind.METHOD ) {
				// should only ever be methods anyway, but...
				continue;
			}

			if ( !member.getEnclosingElement().equals( annotationType ) ) {
				// we only want members declared on the annotation (as opposed to Object e.g.)
				continue;
			}

			ExecutableElement memberAsExecutableElement = (ExecutableElement) member;

			attributeDescriptors.add( new AttributeDescriptor(
					member.getSimpleName().toString(),
					determineType( member ),
					memberAsExecutableElement.getDefaultValue()
			) );
		}

		return attributeDescriptors;
	}

	private Type determineType(Element member) {
		// member should be an ExecutableElement...

		final ExecutableType memberAsExecutableType = (ExecutableType) member.asType();

		return interpretType( memberAsExecutableType.getReturnType() );
	}

	private Type interpretType(TypeMirror type) {
		return switch ( type.getKind() ) {
			case BOOLEAN -> BooleanType.BOOLEAN_TYPE;
			case SHORT -> ShortType.SHORT_TYPE;
			case INT -> IntType.INT_TYPE;
			case LONG -> LongType.LONG_TYPE;
			case DECLARED -> interpretDeclaredType( type );
			case ARRAY -> interpretArrayType( type );
			default -> throw new IllegalStateException();
		};
	}

	private Type interpretDeclaredType(TypeMirror type) {
		final DeclaredType declaredType = (DeclaredType) type;
		final Element declaredTypeAsElement = declaredType.asElement();

		if ( String.class.getName().equals( declaredTypeAsElement.toString() ) ) {
			return STRING_TYPE;
		}

		if ( declaredTypeAsElement.getKind() == ElementKind.ANNOTATION_TYPE ) {
			return new AnnotationType( declaredType );
		}

		if ( declaredTypeAsElement.getKind() == ElementKind.ENUM ) {
			return new EnumType( declaredType );
		}
		return new org.hibernate.orm.build.annotations.structure.DeclaredType( declaredType );
	}

	private Type interpretArrayType(TypeMirror type) {
		final ArrayType arrayType = (ArrayType) type;
		final TypeMirror componentType = arrayType.getComponentType();
		return new org.hibernate.orm.build.annotations.structure.ArrayType( interpretType( componentType ) );
	}

	private String resolveRepeatableContainer(TypeElement annotationClass) {
		// todo : need to resolve this...
		return null;
	}

	private void finishUp() {
//		jpaAnnotationDescriptorMap.forEach( (typeElement, annotationDescriptor) -> {
//			ConcreteClassWriter.writeClass( annotationDescriptor, JPA_CONSTANTS_CLASS, processingEnv );
//		} );
//		ConstantsClassWriter.writeClass(
//				JPA_CONSTANTS_CLASS,
//				jpaAnnotationDescriptorMap,
//				processingEnv
//		);
//
//		hibernateAnnotationDescriptorMap.forEach( (typeElement, annotationDescriptor) -> {
//			ConcreteClassWriter.writeClass( annotationDescriptor, HIBERNATE_CONSTANTS_CLASS, processingEnv );
//		} );
//		ConstantsClassWriter.writeClass(
//				HIBERNATE_CONSTANTS_CLASS,
//				hibernateAnnotationDescriptorMap,
//				processingEnv
//		);
	}
}
