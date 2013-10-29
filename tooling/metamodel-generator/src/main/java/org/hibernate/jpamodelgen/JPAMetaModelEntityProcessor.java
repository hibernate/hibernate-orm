/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.jpamodelgen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
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
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.annotation.AnnotationMetaEntity;
import org.hibernate.jpamodelgen.model.MetaEntity;
import org.hibernate.jpamodelgen.util.Constants;
import org.hibernate.jpamodelgen.util.StringUtil;
import org.hibernate.jpamodelgen.util.TypeUtils;
import org.hibernate.jpamodelgen.xml.JpaDescriptorParser;

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
@SupportedOptions({
		JPAMetaModelEntityProcessor.DEBUG_OPTION,
		JPAMetaModelEntityProcessor.PERSISTENCE_XML_OPTION,
		JPAMetaModelEntityProcessor.ORM_XML_OPTION,
		JPAMetaModelEntityProcessor.FULLY_ANNOTATION_CONFIGURED_OPTION,
		JPAMetaModelEntityProcessor.LAZY_XML_PARSING,
		JPAMetaModelEntityProcessor.ADD_GENERATION_DATE,
		JPAMetaModelEntityProcessor.ADD_GENERATED_ANNOTATION,
		JPAMetaModelEntityProcessor.ADD_SUPPRESS_WARNINGS_ANNOTATION
})
public class JPAMetaModelEntityProcessor extends AbstractProcessor {
	public static final String DEBUG_OPTION = "debug";
	public static final String PERSISTENCE_XML_OPTION = "persistenceXml";
	public static final String ORM_XML_OPTION = "ormXml";
	public static final String FULLY_ANNOTATION_CONFIGURED_OPTION = "fullyAnnotationConfigured";
	public static final String LAZY_XML_PARSING = "lazyXmlParsing";
	public static final String ADD_GENERATION_DATE = "addGenerationDate";
	public static final String ADD_GENERATED_ANNOTATION = "addGeneratedAnnotation";
	public static final String ADD_SUPPRESS_WARNINGS_ANNOTATION = "addSuppressWarningsAnnotation";

	private static final Boolean ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS = Boolean.FALSE;

	private Context context;

	@Override
	public void init(ProcessingEnvironment env) {
		super.init( env );
		context = new Context( env );
		context.logMessage(
				Diagnostic.Kind.NOTE, "Hibernate JPA 2 Static-Metamodel Generator " + Version.getVersionString()
		);

		String tmp = env.getOptions().get( JPAMetaModelEntityProcessor.ADD_GENERATED_ANNOTATION );
		if ( tmp != null ) {
			boolean addGeneratedAnnotation = Boolean.parseBoolean( tmp );
			context.setAddGeneratedAnnotation( addGeneratedAnnotation );
		}

		tmp = env.getOptions().get( JPAMetaModelEntityProcessor.ADD_GENERATION_DATE );
		boolean addGenerationDate = Boolean.parseBoolean( tmp );
		context.setAddGenerationDate( addGenerationDate );

		tmp = env.getOptions().get( JPAMetaModelEntityProcessor.ADD_SUPPRESS_WARNINGS_ANNOTATION );
		boolean addSuppressWarningsAnnotation = Boolean.parseBoolean( tmp );
		context.setAddSuppressWarningsAnnotation( addSuppressWarningsAnnotation );

		tmp = env.getOptions().get( JPAMetaModelEntityProcessor.FULLY_ANNOTATION_CONFIGURED_OPTION );
		boolean fullyAnnotationConfigured = Boolean.parseBoolean( tmp );

		if ( !fullyAnnotationConfigured ) {
			JpaDescriptorParser parser = new JpaDescriptorParser( context );
			parser.parseXml();
			if ( context.isFullyXmlConfigured() ) {
				createMetaModelClasses();
			}
		}
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
		// see also METAGEN-45
		if ( roundEnvironment.processingOver() || annotations.size() == 0 ) {
			return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
		}

		if ( context.isFullyXmlConfigured() ) {
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

		createMetaModelClasses();
		return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
	}

	private void createMetaModelClasses() {
		// keep track of all classes for which model have been generated
		Collection<String> generatedModelClasses = new ArrayList<String>();

		for ( MetaEntity entity : context.getMetaEntities() ) {
			context.logMessage( Diagnostic.Kind.OTHER, "Writing meta model for entity " + entity );
			ClassWriter.writeFile( entity, context );
			generatedModelClasses.add( entity.getQualifiedName() );
		}

		// we cannot process the delayed entities in any order. There might be dependencies between them.
		// we need to process the top level entities first
		Collection<MetaEntity> toProcessEntities = context.getMetaEmbeddables();
		while ( !toProcessEntities.isEmpty() ) {
			Set<MetaEntity> processedEntities = new HashSet<MetaEntity>();
			int toProcessCountBeforeLoop = toProcessEntities.size();
			for ( MetaEntity entity : toProcessEntities ) {
				// see METAGEN-36
				if ( generatedModelClasses.contains( entity.getQualifiedName() ) ) {
					processedEntities.add( entity );
					continue;
				}
				if ( modelGenerationNeedsToBeDeferred( toProcessEntities, entity ) ) {
					continue;
				}
				context.logMessage(
						Diagnostic.Kind.OTHER, "Writing meta model for embeddable/mapped superclass" + entity
				);
				ClassWriter.writeFile( entity, context );
				processedEntities.add( entity );
			}
			toProcessEntities.removeAll( processedEntities );
			if ( toProcessEntities.size() >= toProcessCountBeforeLoop ) {
				context.logMessage(
						Diagnostic.Kind.ERROR, "Potential endless loop in generation of entities."
				);
			}
		}
	}

	private boolean modelGenerationNeedsToBeDeferred(Collection<MetaEntity> entities, MetaEntity containedEntity) {
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
		return TypeUtils.containsAnnotation(
				element,
				Constants.ENTITY,
				Constants.MAPPED_SUPERCLASS,
				Constants.EMBEDDABLE
		);
	}

	private void handleRootElementAnnotationMirrors(final Element element) {
		List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
		for ( AnnotationMirror mirror : annotationMirrors ) {
			if ( !ElementKind.CLASS.equals( element.getKind() ) ) {
				continue;
			}

			String fqn = ( (TypeElement) element ).getQualifiedName().toString();
			MetaEntity alreadyExistingMetaEntity = tryGettingExistingEntityFromContext( mirror, fqn );
			if ( alreadyExistingMetaEntity != null && alreadyExistingMetaEntity.isMetaComplete() ) {
				String msg = "Skipping processing of annotations for " + fqn + " since xml configuration is metadata complete.";
				context.logMessage( Diagnostic.Kind.OTHER, msg );
				continue;
			}

			boolean requiresLazyMemberInitialization = false;
			AnnotationMetaEntity metaEntity;
			if ( TypeUtils.containsAnnotation( element, Constants.EMBEDDABLE ) ||
					TypeUtils.containsAnnotation( element, Constants.MAPPED_SUPERCLASS ) ) {
				requiresLazyMemberInitialization = true;
			}

			metaEntity = new AnnotationMetaEntity( (TypeElement) element, context, requiresLazyMemberInitialization );

			if ( alreadyExistingMetaEntity != null ) {
				metaEntity.mergeInMembers( alreadyExistingMetaEntity );
			}
			addMetaEntityToContext( mirror, metaEntity );
		}
	}

	private MetaEntity tryGettingExistingEntityFromContext(AnnotationMirror mirror, String fqn) {
		MetaEntity alreadyExistingMetaEntity = null;
		if ( TypeUtils.isAnnotationMirrorOfType( mirror, Constants.ENTITY )
				|| TypeUtils.isAnnotationMirrorOfType( mirror, Constants.MAPPED_SUPERCLASS )) {
			alreadyExistingMetaEntity = context.getMetaEntity( fqn );
		}
		else if ( TypeUtils.isAnnotationMirrorOfType( mirror, Constants.EMBEDDABLE ) ) {
			alreadyExistingMetaEntity = context.getMetaEmbeddable( fqn );
		}
		return alreadyExistingMetaEntity;
	}

	private void addMetaEntityToContext(AnnotationMirror mirror, AnnotationMetaEntity metaEntity) {
		if ( TypeUtils.isAnnotationMirrorOfType( mirror, Constants.ENTITY ) ) {
			context.addMetaEntity( metaEntity.getQualifiedName(), metaEntity );
		}
		else if ( TypeUtils.isAnnotationMirrorOfType( mirror, Constants.MAPPED_SUPERCLASS ) ) {
			context.addMetaEntity( metaEntity.getQualifiedName(), metaEntity );
		}
		else if ( TypeUtils.isAnnotationMirrorOfType( mirror, Constants.EMBEDDABLE ) ) {
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
			TypeElement returnedElement = (TypeElement) context.getTypeUtils().asElement( declaredType );

			String fqNameOfReturnType = returnedElement.getQualifiedName().toString();
			String collection = Constants.COLLECTIONS.get( fqNameOfReturnType );
			if ( collection != null ) {
				TypeMirror collectionElementType = TypeUtils.getCollectionElementType(
						declaredType, fqNameOfReturnType, null, context
				);
				returnedElement = (TypeElement) context.getTypeUtils().asElement( collectionElementType );
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
			if ( !StringUtil.isProperty( string, TypeUtils.toTypeString( t.getReturnType() ) ) ) {
				return Boolean.FALSE;
			}

			TypeMirror returnType = t.getReturnType();
			return returnType.accept( this, element );
		}
	}
}
