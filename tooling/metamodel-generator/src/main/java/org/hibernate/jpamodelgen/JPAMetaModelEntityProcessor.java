/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

// $Id$

package org.hibernate.jpamodelgen;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.annotation.AnnotationEmbeddable;
import org.hibernate.jpamodelgen.annotation.AnnotationMetaEntity;
import org.hibernate.jpamodelgen.model.MetaEntity;
import org.hibernate.jpamodelgen.util.Constants;
import org.hibernate.jpamodelgen.util.StringUtil;
import org.hibernate.jpamodelgen.util.TypeUtils;
import org.hibernate.jpamodelgen.xml.XmlParser;

import static javax.lang.model.SourceVersion.RELEASE_6;

/**
 * Main annotation processor.
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
@SupportedAnnotationTypes({
		"javax.persistence.Entity", "javax.persistence.MappedSuperclass", "javax.persistence.Embeddable"
})
@SupportedSourceVersion(RELEASE_6)
@SupportedOptions({
		JPAMetaModelEntityProcessor.DEBUG_OPTION,
		JPAMetaModelEntityProcessor.PERSISTENCE_XML_OPTION,
		JPAMetaModelEntityProcessor.ORM_XML_OPTION,
		JPAMetaModelEntityProcessor.FULLY_ANNOTATION_CONFIGURED_OPTION,
		JPAMetaModelEntityProcessor.LAZY_XML_PARSING
})
public class JPAMetaModelEntityProcessor extends AbstractProcessor {
	public static final String DEBUG_OPTION = "debug";
	public static final String PERSISTENCE_XML_OPTION = "persistenceXml";
	public static final String ORM_XML_OPTION = "ormXmlList";
	public static final String FULLY_ANNOTATION_CONFIGURED_OPTION = "fullyAnnotationConfigured";
	public static final String LAZY_XML_PARSING = "lazyXmlParsing";

	private static final Boolean ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS = Boolean.FALSE;

	private Context context;

	public void init(ProcessingEnvironment env) {
		super.init( env );
		context = new Context( env );
		context.logMessage(
				Diagnostic.Kind.NOTE, "Hibernate JPA 2 Static-Metamodel Generator " + Version.getVersionString()
		);

		String tmp = env.getOptions().get( JPAMetaModelEntityProcessor.FULLY_ANNOTATION_CONFIGURED_OPTION );
		boolean fullyAnnotationConfigured = Boolean.parseBoolean( tmp );

		if ( !fullyAnnotationConfigured ) {
			XmlParser parser = new XmlParser( context );
			parser.parseXml();
			if ( context.isPersistenceUnitCompletelyXmlConfigured() ) {
				createMetaModelClasses();
			}
		}
	}

	@Override
	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
		if ( roundEnvironment.processingOver() ) {
			if ( !context.isPersistenceUnitCompletelyXmlConfigured() ) {
				context.logMessage( Diagnostic.Kind.OTHER, "Last processing round." );
				createMetaModelClasses();
				context.logMessage( Diagnostic.Kind.OTHER, "Finished processing" );
			}
			return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
		}

		if ( context.isPersistenceUnitCompletelyXmlConfigured() ) {
			context.logMessage(
					Diagnostic.Kind.OTHER,
					"Skipping the processing of annotations since persistence unit is purely xml configured."
			);
			return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
		}

		Set<? extends Element> elements = roundEnvironment.getRootElements();
		for ( Element element : elements ) {
			if ( isJPAEntity( element ) ) {
				context.logMessage( Diagnostic.Kind.OTHER, "Processing annotated class " + element.toString() );
				handleRootElementAnnotationMirrors( element );
			}
		}

		return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
	}

	private void createMetaModelClasses() {
		for ( MetaEntity entity : context.getMetaEntities() ) {
			context.logMessage( Diagnostic.Kind.OTHER, "Writing meta model for entity " + entity );
			ClassWriter.writeFile( entity, context );
		}

		// we cannot process the delayed entities in any order. There might be dependencies between them.
		// we need to process the top level entities first
		// TODO make sure that we don't run into circular dependencies here
		Collection<MetaEntity> toProcessEntities = context.getMetaEmbeddables();
		while ( !toProcessEntities.isEmpty() ) {
			Set<MetaEntity> processedEntities = new HashSet<MetaEntity>();
			for ( MetaEntity entity : toProcessEntities ) {
				if ( containedInEntity( toProcessEntities, entity ) ) {
					continue;
				}
				context.logMessage(
						Diagnostic.Kind.OTHER, "Writing meta model for embeddable/mapped superclass" + entity
				);
				ClassWriter.writeFile( entity, context );
				processedEntities.add( entity );
			}
			toProcessEntities.removeAll( processedEntities );
		}
	}

	private boolean containedInEntity(Collection<MetaEntity> entities, MetaEntity containedEntity) {
		ContainsAttributeTypeVisitor visitor = new ContainsAttributeTypeVisitor(
				containedEntity.getTypeElement(), context
		);
		for ( MetaEntity entity : entities ) {
			if ( entity.equals( containedEntity ) ) {
				continue;
			}
			for ( Element subElement : ElementFilter.fieldsIn( entity.getTypeElement().getEnclosedElements() ) ) {
				TypeMirror mirror = subElement.asType();
				if ( !TypeKind.DECLARED.equals( mirror.getKind() ) ) {
					continue;
				}
				boolean contains = mirror.accept( visitor, subElement );
				if ( contains ) {
					return true;
				}
			}
			for ( Element subElement : ElementFilter.methodsIn( entity.getTypeElement().getEnclosedElements() ) ) {
				TypeMirror mirror = subElement.asType();
				if ( !TypeKind.DECLARED.equals( mirror.getKind() ) ) {
					continue;
				}
				boolean contains = mirror.accept( visitor, subElement );
				if ( contains ) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isJPAEntity(Element element) {
		return TypeUtils.containsAnnotation( element, Entity.class, MappedSuperclass.class, Embeddable.class );
	}

	private void handleRootElementAnnotationMirrors(final Element element) {
		List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
		for ( AnnotationMirror mirror : annotationMirrors ) {
			if ( !ElementKind.CLASS.equals( element.getKind() ) ) {
				continue;
			}

			String fqn = ( ( TypeElement ) element ).getQualifiedName().toString();
			MetaEntity alreadyExistingMetaEntity = tryGettingExistingEntityFromContext( mirror, fqn );
			if ( alreadyExistingMetaEntity != null && alreadyExistingMetaEntity.isMetaComplete() ) {
				String msg = "Skipping processing of annotations for " + fqn + " since xml configuration is metadata complete.";
				context.logMessage( Diagnostic.Kind.OTHER, msg );
				continue;
			}

			AnnotationMetaEntity metaEntity;
			if ( TypeUtils.containsAnnotation( element, Embeddable.class ) ) {
				metaEntity = new AnnotationEmbeddable( ( TypeElement ) element, context );
			}
			else {
				metaEntity = new AnnotationMetaEntity( ( TypeElement ) element, context );
			}

			if ( alreadyExistingMetaEntity != null ) {
				metaEntity.mergeInMembers( alreadyExistingMetaEntity.getMembers() );
			}
			addMetaEntityToContext( mirror, metaEntity );
		}
	}

	private MetaEntity tryGettingExistingEntityFromContext(AnnotationMirror mirror, String fqn) {
		MetaEntity alreadyExistingMetaEntity = null;
		if ( TypeUtils.isAnnotationMirrorOfType( mirror, Entity.class ) ) {
			alreadyExistingMetaEntity = context.getMetaEntity( fqn );
		}
		else if ( TypeUtils.isAnnotationMirrorOfType( mirror, MappedSuperclass.class )
				|| TypeUtils.isAnnotationMirrorOfType( mirror, Embeddable.class ) ) {
			alreadyExistingMetaEntity = context.getMetaEmbeddable( fqn );
		}
		return alreadyExistingMetaEntity;
	}

	private void addMetaEntityToContext(AnnotationMirror mirror, AnnotationMetaEntity metaEntity) {
		if ( TypeUtils.isAnnotationMirrorOfType( mirror, Entity.class ) ) {
			context.addMetaEntity( metaEntity.getQualifiedName(), metaEntity );
		}
		else if ( TypeUtils.isAnnotationMirrorOfType( mirror, MappedSuperclass.class ) ) {
			context.addMetaEntity( metaEntity.getQualifiedName(), metaEntity );
		}
		else if ( TypeUtils.isAnnotationMirrorOfType( mirror, Embeddable.class ) ) {
			context.addMetaEmbeddable( metaEntity.getQualifiedName(), metaEntity );
		}
	}


	class ContainsAttributeTypeVisitor extends SimpleTypeVisitor6<Boolean, Element> {

		private Context context;
		private TypeElement type;

		ContainsAttributeTypeVisitor(TypeElement elem, Context context) {
			this.context = context;
			this.type = elem;
		}

		@Override
		public Boolean visitDeclared(DeclaredType declaredType, Element element) {
			TypeElement returnedElement = ( TypeElement ) context.getTypeUtils().asElement( declaredType );

			String fqNameOfReturnType = returnedElement.getQualifiedName().toString();
			String collection = Constants.COLLECTIONS.get( fqNameOfReturnType );
			if ( collection != null ) {
				TypeMirror collectionElementType = TypeUtils.getCollectionElementType(
						declaredType, fqNameOfReturnType, null, context
				);
				returnedElement = ( TypeElement ) context.getTypeUtils().asElement( collectionElementType );
			}

			if ( type.getQualifiedName().toString().equals( returnedElement.getQualifiedName().toString() ) ) {
				return Boolean.TRUE;
			}
			else {
				return Boolean.FALSE;
			}
		}

		@Override
		public Boolean visitExecutable(ExecutableType t, Element element) {
			if ( !element.getKind().equals( ElementKind.METHOD ) ) {
				return Boolean.FALSE;
			}

			String string = element.getSimpleName().toString();
			if ( !StringUtil.isPropertyName( string ) ) {
				return Boolean.FALSE;
			}

			TypeMirror returnType = t.getReturnType();
			return returnType.accept( this, element );
		}
	}
}
