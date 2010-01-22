// $Id$
/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.jpamodelgen;

import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.annotation.AnnotationMetaEntity;
import org.hibernate.jpamodelgen.xml.XmlParser;

import static javax.lang.model.SourceVersion.RELEASE_6;

/**
 * Main annotation processor.
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(RELEASE_6)
public class JPAMetaModelEntityProcessor extends AbstractProcessor {


	private static final Boolean ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS = Boolean.FALSE;
	private static final String ENTITY_ANN = javax.persistence.Entity.class.getName();
	private static final String MAPPED_SUPERCLASS_ANN = MappedSuperclass.class.getName();
	private static final String EMBEDDABLE_ANN = Embeddable.class.getName();

	private boolean xmlProcessed = false;
	private Context context;

	public void init(ProcessingEnvironment env) {
		super.init( env );
		context = new Context( env );
		context.logMessage(
				Diagnostic.Kind.NOTE, "Hibernate JPA 2 Static-Metamodel Generator " + Version.getVersionString()
		);
	}

	@Override
	public boolean process(final Set<? extends TypeElement> annotations,
						   final RoundEnvironment roundEnvironment) {

		if ( roundEnvironment.processingOver() ) {
			context.logMessage( Diagnostic.Kind.OTHER, "Last processing round." );
			createMetaModelClasses();
			context.logMessage( Diagnostic.Kind.OTHER, "Finished processing" );
			return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
		}

		if ( !xmlProcessed ) {
			XmlParser parser = new XmlParser( context );
			parser.parsePersistenceXml();
			xmlProcessed = true;
		}

		if ( !hostJPAAnnotations( annotations ) ) {
			context.logMessage( Diagnostic.Kind.OTHER, "Current processing round does not contain entities" );
			return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
		}

		Set<? extends Element> elements = roundEnvironment.getRootElements();
		for ( Element element : elements ) {
			context.logMessage( Diagnostic.Kind.OTHER, "Processing " + element.toString() );
			handleRootElementAnnotationMirrors( element );
		}

		return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
	}

	private void createMetaModelClasses() {
		for ( MetaEntity entity : context.getMetaEntitiesToProcess().values() ) {
			context.logMessage( Diagnostic.Kind.OTHER, "Writing meta model for " + entity );
			ClassWriter.writeFile( entity, context );
		}

		//process left over, in most cases is empty
		for ( String className : context.getElementsAlreadyProcessed() ) {
			context.getMetaSuperclassAndEmbeddableToProcess().remove( className );
		}

		for ( MetaEntity entity : context.getMetaSuperclassAndEmbeddableToProcess().values() ) {
			context.logMessage( Diagnostic.Kind.OTHER, "Writing meta model for " + entity );
			ClassWriter.writeFile( entity, context );
		}
	}

	private boolean hostJPAAnnotations(Set<? extends TypeElement> annotations) {
		for ( TypeElement type : annotations ) {
			final String typeName = type.getQualifiedName().toString();
			if ( typeName.equals( ENTITY_ANN ) ) {
				return true;
			}
			else if ( typeName.equals( EMBEDDABLE_ANN ) ) {
				return true;
			}
			else if ( typeName.equals( MAPPED_SUPERCLASS_ANN ) ) {
				return true;
			}
		}
		return false;
	}

	private void handleRootElementAnnotationMirrors(final Element element) {

		List<? extends AnnotationMirror> annotationMirrors = element
				.getAnnotationMirrors();

		for ( AnnotationMirror mirror : annotationMirrors ) {
			final String annotationType = mirror.getAnnotationType().toString();

			if ( element.getKind() == ElementKind.CLASS ) {
				if ( annotationType.equals( ENTITY_ANN ) ) {
					AnnotationMetaEntity metaEntity = new AnnotationMetaEntity( ( TypeElement ) element, context );
					// TODO instead of just adding the entity we have to do some merging.
					context.getMetaEntitiesToProcess().put( metaEntity.getQualifiedName(), metaEntity );
				}
				else if ( annotationType.equals( MAPPED_SUPERCLASS_ANN )
						|| annotationType.equals( EMBEDDABLE_ANN ) ) {
					AnnotationMetaEntity metaEntity = new AnnotationMetaEntity( ( TypeElement ) element, context );

					// TODO instead of just adding the entity we have to do some merging.
					context.getMetaSuperclassAndEmbeddableToProcess().put( metaEntity.getQualifiedName(), metaEntity );
				}
			}
		}
	}
}
