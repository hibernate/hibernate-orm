/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
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
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.annotation.AnnotationMetaEntity;
import org.hibernate.jpamodelgen.annotation.AnnotationMetaPackage;
import org.hibernate.jpamodelgen.model.Metamodel;
import org.hibernate.jpamodelgen.util.Constants;
import org.hibernate.jpamodelgen.xml.JpaDescriptorParser;

import org.checkerframework.checker.nullness.qual.Nullable;

import static java.lang.Boolean.parseBoolean;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static org.hibernate.jpamodelgen.util.Constants.FIND;
import static org.hibernate.jpamodelgen.util.Constants.HQL;
import static org.hibernate.jpamodelgen.util.Constants.SQL;
import static org.hibernate.jpamodelgen.util.StringUtil.isProperty;
import static org.hibernate.jpamodelgen.util.TypeUtils.*;

/**
 * Main annotation processor.
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
@SupportedAnnotationTypes({
		Constants.ENTITY,
		Constants.MAPPED_SUPERCLASS,
		Constants.EMBEDDABLE,
		Constants.HQL,
		Constants.SQL,
		Constants.FIND,
		Constants.NAMED_QUERY,
		Constants.NAMED_NATIVE_QUERY,
		Constants.NAMED_ENTITY_GRAPH,
		Constants.SQL_RESULT_SET_MAPPING,
		Constants.HIB_FETCH_PROFILE,
		Constants.HIB_FILTER_DEF,
		Constants.HIB_NAMED_QUERY,
		Constants.HIB_NAMED_NATIVE_QUERY
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

	private static final boolean ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS = false;

	private Context context;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnvironment) {
		super.init( processingEnvironment );
		context = new Context( processingEnvironment );
		context.logMessage(
				Diagnostic.Kind.NOTE,
				"Hibernate/JPA static Metamodel Generator " + Version.getVersionString()
		);

		boolean fullyAnnotationConfigured = handleSettings( processingEnvironment );
		if ( !fullyAnnotationConfigured ) {
			new JpaDescriptorParser( context ).parseXml();
			if ( context.isFullyXmlConfigured() ) {
				createMetaModelClasses();
			}
		}
	}

	private boolean handleSettings(ProcessingEnvironment environment) {
		final PackageElement jakartaInjectPackage =
				context.getProcessingEnvironment().getElementUtils()
						.getPackageElement( "jakarta.inject" );
		final PackageElement jakartaAnnotationPackage =
				context.getProcessingEnvironment().getElementUtils()
						.getPackageElement( "jakarta.annotation" );
		final PackageElement jakartaContextPackage =
				context.getProcessingEnvironment().getElementUtils()
						.getPackageElement( "jakarta.enterprise.context" );

		context.setAddInjectAnnotation( jakartaInjectPackage != null );
		context.setAddNonnullAnnotation( jakartaAnnotationPackage != null );
		context.setAddGeneratedAnnotation( jakartaAnnotationPackage != null );
		context.setAddDependentAnnotation( jakartaContextPackage != null );

		final Map<String, String> options = environment.getOptions();

		String setting = options.get( ADD_GENERATED_ANNOTATION );
		if ( setting != null ) {
			context.setAddGeneratedAnnotation( parseBoolean( setting ) );
		}

		context.setAddGenerationDate( parseBoolean( options.get( ADD_GENERATION_DATE ) ) );

		context.setAddSuppressWarningsAnnotation( parseBoolean( options.get( ADD_SUPPRESS_WARNINGS_ANNOTATION ) ) );

		return parseBoolean( options.get( FULLY_ANNOTATION_CONFIGURED_OPTION ) );
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {

		// https://hibernate.atlassian.net/browse/METAGEN-45 claims that we need
		// if ( roundEnvironment.processingOver() || annotations.size() == 0)
		// but that was back on JDK 6 and I don't see why it should be necessary
		// - in fact we want to use the last round to run the 'elementsToRedo'
		if ( roundEnvironment.processingOver() ) {
			final Set<CharSequence> elementsToRedo = context.getElementsToRedo();
			if ( !elementsToRedo.isEmpty() ) {
				context.logMessage( Diagnostic.Kind.ERROR, "Failed to generate code for " + elementsToRedo );
			}
		}
		else if ( context.isFullyXmlConfigured() ) {
			context.logMessage(
					Diagnostic.Kind.OTHER,
					"Skipping the processing of annotations since persistence unit is purely XML configured."
			);
		}
		else {
			context.logMessage( Diagnostic.Kind.OTHER, "Starting new round" );
			try {
				processClasses( roundEnvironment );
				createMetaModelClasses();
			}
			catch (Exception e) {
				context.logMessage( Diagnostic.Kind.ERROR, "Error generating JPA metamodel: " + e.getMessage() );
			}
		}
		return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
	}

	private void processClasses(RoundEnvironment roundEnvironment) {
		for ( CharSequence elementName : new HashSet<>( context.getElementsToRedo() ) ) {
			context.logMessage( Diagnostic.Kind.OTHER, "Redoing element '" + elementName + "'" );
			final TypeElement typeElement = context.getElementUtils().getTypeElement( elementName );
			try {
				final AnnotationMetaEntity metaEntity =
						AnnotationMetaEntity.create( typeElement, context, false );
				context.addMetaAuxiliary( metaEntity.getQualifiedName(), metaEntity );
				context.removeElementToRedo( elementName );
			}
			catch (ProcessLaterException processLaterException) {
				// leave it there for next time
			}
		}

		for ( Element element : roundEnvironment.getRootElements() ) {
			try {
				if ( isJPAEntity( element ) ) {
					context.logMessage( Diagnostic.Kind.OTHER, "Processing annotated entity class '" + element + "'" );
					handleRootElementAnnotationMirrors( element );
				}
				else if ( hasAuxiliaryAnnotations( element ) ) {
					context.logMessage( Diagnostic.Kind.OTHER, "Processing annotated class '" + element + "'" );
					handleRootElementAuxiliaryAnnotationMirrors( element );
				}
				else if ( element instanceof TypeElement ) {
					final TypeElement typeElement = (TypeElement) element;
						for ( Element member : typeElement.getEnclosedElements() ) {
							if ( containsAnnotation( member, HQL, SQL, FIND ) ) {
								context.logMessage( Diagnostic.Kind.OTHER, "Processing annotated class '" + element + "'" );
								final AnnotationMetaEntity metaEntity =
										AnnotationMetaEntity.create( typeElement, context, false );
								context.addMetaAuxiliary( metaEntity.getQualifiedName(), metaEntity );
								break;
							}
						}
				}
			}
			catch ( ProcessLaterException processLaterException ) {
				if ( element instanceof TypeElement ) {
					context.logMessage(
							Diagnostic.Kind.OTHER,
							"Could not process '" + element + "' (will redo in next round)"
					);
					context.addElementToRedo( ( (TypeElement) element).getQualifiedName() );
				}
			}
		}
	}

	private void createMetaModelClasses() {

		for ( Metamodel aux : context.getMetaAuxiliaries() ) {
			if ( context.isAlreadyGenerated( aux.getQualifiedName() ) ) {
				continue;
			}
			context.logMessage( Diagnostic.Kind.OTHER, "Writing metamodel for auxiliary '" + aux + "'" );
			ClassWriter.writeFile( aux, context );
			context.markGenerated( aux.getQualifiedName() );
		}

		for ( Metamodel entity : context.getMetaEntities() ) {
			if ( context.isAlreadyGenerated( entity.getQualifiedName() ) ) {
				continue;
			}
			context.logMessage( Diagnostic.Kind.OTHER, "Writing metamodel for entity '" + entity + "'" );
			ClassWriter.writeFile( entity, context );
			context.markGenerated( entity.getQualifiedName() );
		}

		// we cannot process the delayed entities in any order. There might be dependencies between them.
		// we need to process the top level entities first
		final Collection<Metamodel> toProcessEntities = context.getMetaEmbeddables();
		while ( !toProcessEntities.isEmpty() ) {
			final Set<Metamodel> processedEntities = new HashSet<>();
			int toProcessCountBeforeLoop = toProcessEntities.size();
			for ( Metamodel entity : toProcessEntities ) {
				// see METAGEN-36
				if ( context.isAlreadyGenerated( entity.getQualifiedName() ) ) {
					processedEntities.add( entity );
					continue;
				}
				if ( modelGenerationNeedsToBeDeferred( toProcessEntities, entity ) ) {
					continue;
				}
				context.logMessage(
						Diagnostic.Kind.OTHER,
						"Writing meta model for embeddable/mapped superclass " + entity
				);
				ClassWriter.writeFile( entity, context );
				context.markGenerated( entity.getQualifiedName() );
				processedEntities.add( entity );
			}
			toProcessEntities.removeAll( processedEntities );
			if ( toProcessEntities.size() >= toProcessCountBeforeLoop ) {
				context.logMessage(
						Diagnostic.Kind.ERROR,
						"Potential endless loop in generation of entities."
				);
			}
		}
	}

	private boolean modelGenerationNeedsToBeDeferred(Collection<Metamodel> entities, Metamodel containedEntity) {
		final Element element = containedEntity.getElement();
		if ( element instanceof TypeElement ) {
			ContainsAttributeTypeVisitor visitor = new ContainsAttributeTypeVisitor( (TypeElement) element, context );
			for ( Metamodel entity : entities ) {
				if ( entity.equals( containedEntity ) ) {
					continue;
				}
				for ( Element subElement : fieldsIn( entity.getElement().getEnclosedElements() ) ) {
					TypeMirror mirror = subElement.asType();
					if ( TypeKind.DECLARED == mirror.getKind() ) {
						if ( mirror.accept( visitor, subElement ) ) {
							return true;
						}
					}
				}
				for ( Element subElement : methodsIn( entity.getElement().getEnclosedElements() ) ) {
					TypeMirror mirror = subElement.asType();
					if ( TypeKind.DECLARED == mirror.getKind() ) {
						if ( mirror.accept( visitor, subElement ) ) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean isJPAEntity(Element element) {
		return containsAnnotation(
				element,
				Constants.ENTITY,
				Constants.MAPPED_SUPERCLASS,
				Constants.EMBEDDABLE
		);
	}

	private boolean hasAuxiliaryAnnotations(Element element) {
		return containsAnnotation(
				element,
				Constants.NAMED_QUERY,
				Constants.NAMED_QUERIES,
				Constants.NAMED_NATIVE_QUERY,
				Constants.NAMED_NATIVE_QUERIES,
				Constants.SQL_RESULT_SET_MAPPING,
				Constants.SQL_RESULT_SET_MAPPINGS,
				Constants.NAMED_ENTITY_GRAPH,
				Constants.NAMED_ENTITY_GRAPHS,
				Constants.HIB_NAMED_QUERY,
				Constants.HIB_NAMED_QUERIES,
				Constants.HIB_NAMED_NATIVE_QUERY,
				Constants.HIB_NAMED_NATIVE_QUERIES,
				Constants.HIB_FETCH_PROFILE,
				Constants.HIB_FETCH_PROFILES,
				Constants.HIB_FILTER_DEF,
				Constants.HIB_FILTER_DEFS
		);
	}

	private void handleRootElementAnnotationMirrors(final Element element) {
		for ( AnnotationMirror mirror : element.getAnnotationMirrors() ) {
			if ( element.getKind() == ElementKind.CLASS ) {
				final String qualifiedName = ( (TypeElement) element ).getQualifiedName().toString();
				final Metamodel alreadyExistingMetaEntity = tryGettingExistingEntityFromContext( mirror, qualifiedName );
				if ( alreadyExistingMetaEntity != null && alreadyExistingMetaEntity.isMetaComplete() ) {
					context.logMessage(
							Diagnostic.Kind.OTHER,
							"Skipping processing of annotations for '" + qualifiedName
									+ "' since xml configuration is metadata complete.");
				}
				else {
					boolean requiresLazyMemberInitialization
							= containsAnnotation( element, Constants.EMBEDDABLE )
							|| containsAnnotation( element, Constants.MAPPED_SUPERCLASS );
					final AnnotationMetaEntity metaEntity =
							AnnotationMetaEntity.create( (TypeElement) element, context, requiresLazyMemberInitialization );
					if ( alreadyExistingMetaEntity != null ) {
						metaEntity.mergeInMembers( alreadyExistingMetaEntity );
					}
					addMetaEntityToContext( mirror, metaEntity );
				}
			}
		}
	}

	private void handleRootElementAuxiliaryAnnotationMirrors(final Element element) {
		if ( element instanceof TypeElement ) {
			final AnnotationMetaEntity metaEntity =
					AnnotationMetaEntity.create( (TypeElement) element, context, false );
			context.addMetaAuxiliary( metaEntity.getQualifiedName(), metaEntity );
		}
		else if ( element instanceof PackageElement ) {
			final AnnotationMetaPackage metaEntity =
					AnnotationMetaPackage.create( (PackageElement) element, context );
			context.addMetaAuxiliary( metaEntity.getQualifiedName(), metaEntity );
		}
		//TODO: handle PackageElement
	}

	private @Nullable Metamodel tryGettingExistingEntityFromContext(AnnotationMirror mirror, String qualifiedName) {
		if ( isAnnotationMirrorOfType( mirror, Constants.ENTITY )
				|| isAnnotationMirrorOfType( mirror, Constants.MAPPED_SUPERCLASS ) ) {
			return context.getMetaEntity( qualifiedName );
		}
		else if ( isAnnotationMirrorOfType( mirror, Constants.EMBEDDABLE ) ) {
			return context.getMetaEmbeddable( qualifiedName );
		}
		return null;
	}

	private void addMetaEntityToContext(AnnotationMirror mirror, AnnotationMetaEntity metaEntity) {
		if ( isAnnotationMirrorOfType( mirror, Constants.ENTITY ) ) {
			context.addMetaEntity( metaEntity.getQualifiedName(), metaEntity );
		}
		else if ( isAnnotationMirrorOfType( mirror, Constants.MAPPED_SUPERCLASS ) ) {
			context.addMetaEntity( metaEntity.getQualifiedName(), metaEntity );
		}
		else if ( isAnnotationMirrorOfType( mirror, Constants.EMBEDDABLE ) ) {
			context.addMetaEmbeddable( metaEntity.getQualifiedName(), metaEntity );
		}
	}


	static class ContainsAttributeTypeVisitor extends SimpleTypeVisitor8<Boolean, Element> {

		private final Context context;
		private final TypeElement type;

		ContainsAttributeTypeVisitor(TypeElement elem, Context context) {
			this.context = context;
			this.type = elem;
		}

		@Override
		public Boolean visitDeclared(DeclaredType declaredType, Element element) {
			TypeElement returnedElement = (TypeElement) context.getTypeUtils().asElement( declaredType );

			final String fqNameOfReturnType = returnedElement.getQualifiedName().toString();
			final String collection = Constants.COLLECTIONS.get( fqNameOfReturnType );
			if ( collection != null ) {
				final TypeMirror collectionElementType =
						getCollectionElementType( declaredType, fqNameOfReturnType, null, context );
				final Element collectionElement = context.getTypeUtils().asElement( collectionElementType );
				if ( ElementKind.TYPE_PARAMETER.equals( collectionElement.getKind() ) ) {
					return false;
				}
				returnedElement = (TypeElement) collectionElement;
			}

			return type.getQualifiedName().contentEquals( returnedElement.getQualifiedName() );
		}

		@Override
		public Boolean visitExecutable(ExecutableType t, Element element) {
			return element.getKind().equals(ElementKind.METHOD)
				&& isProperty( element.getSimpleName().toString(), toTypeString( t.getReturnType() ) )
				&& t.getReturnType().accept( this, element );
		}
	}
}
