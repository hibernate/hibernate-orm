/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.processor.annotation.AnnotationMetaEntity;
import org.hibernate.processor.annotation.AnnotationMetaPackage;
import org.hibernate.processor.model.Metamodel;
import org.hibernate.processor.xml.JpaDescriptorParser;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Boolean.parseBoolean;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static org.hibernate.processor.HibernateProcessor.ADD_GENERATED_ANNOTATION;
import static org.hibernate.processor.HibernateProcessor.ADD_GENERATION_DATE;
import static org.hibernate.processor.HibernateProcessor.ADD_SUPPRESS_WARNINGS_ANNOTATION;
import static org.hibernate.processor.HibernateProcessor.DEBUG_OPTION;
import static org.hibernate.processor.HibernateProcessor.FULLY_ANNOTATION_CONFIGURED_OPTION;
import static org.hibernate.processor.HibernateProcessor.LAZY_XML_PARSING;
import static org.hibernate.processor.HibernateProcessor.ORM_XML_OPTION;
import static org.hibernate.processor.HibernateProcessor.PERSISTENCE_XML_OPTION;
import static org.hibernate.processor.HibernateProcessor.SUPPRESS_JAKARTA_DATA_METAMODEL;
import static org.hibernate.processor.util.Constants.*;
import static org.hibernate.processor.util.TypeUtils.containsAnnotation;
import static org.hibernate.processor.util.TypeUtils.getAnnotationMirror;
import static org.hibernate.processor.util.TypeUtils.getAnnotationValue;
import static org.hibernate.processor.util.TypeUtils.hasAnnotation;
import static org.hibernate.processor.util.TypeUtils.isClassOrRecordType;

/**
 * Main annotation processor.
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 * @author Gavin King
 */
@SupportedAnnotationTypes({
		// standard for JPA 2
		ENTITY, MAPPED_SUPERCLASS, EMBEDDABLE,
		// standard for JPA 3.2
		NAMED_QUERY, NAMED_NATIVE_QUERY, NAMED_ENTITY_GRAPH, SQL_RESULT_SET_MAPPING,
		// extra for Hibernate
		HIB_FETCH_PROFILE, HIB_FILTER_DEF, HIB_NAMED_QUERY, HIB_NAMED_NATIVE_QUERY,
		// Hibernate query methods
		HQL, SQL, FIND,
		// Jakarta Data repositories
		JD_REPOSITORY // do not need to list any other Jakarta Data annotations here
})
@SupportedOptions({
		DEBUG_OPTION,
		PERSISTENCE_XML_OPTION,
		ORM_XML_OPTION,
		FULLY_ANNOTATION_CONFIGURED_OPTION,
		LAZY_XML_PARSING,
		ADD_GENERATION_DATE,
		ADD_GENERATED_ANNOTATION,
		ADD_SUPPRESS_WARNINGS_ANNOTATION,
		SUPPRESS_JAKARTA_DATA_METAMODEL
})
public class HibernateProcessor extends AbstractProcessor {

	/**
	 * Debug logging from the processor
	 */
	public static final String DEBUG_OPTION = "debug";

	/**
	 * Path to a {@code persistence.xml} file
	 */
	public static final String PERSISTENCE_XML_OPTION = "persistenceXml";

	/**
	 * Path to an {@code orm.xml} file
	 */
	public static final String ORM_XML_OPTION = "ormXml";

	/**
	 * Controls whether the processor should consider XML files
	 */
	public static final String FULLY_ANNOTATION_CONFIGURED_OPTION = "fullyAnnotationConfigured";

	/**
	 * Controls whether the processor should only load XML files when there have been changes
	 */
	public static final String LAZY_XML_PARSING = "lazyXmlParsing";

	/**
	 * Whether the {@code jakarta.annotation.Generated} annotation should be added to
	 * the generated classes
	 */
	public static final String ADD_GENERATED_ANNOTATION = "addGeneratedAnnotation";

	/**
	 * Assuming that {@linkplain #ADD_GENERATED_ANNOTATION} is enabled, this option controls
	 * whether {@code @Generated#date} should be populated.
	 */
	public static final String ADD_GENERATION_DATE = "addGenerationDate";

	/**
	 * Controls whether {@code @SuppressWarnings({"deprecation","rawtypes"})} should be added to the generated classes
	 */
	public static final String ADD_SUPPRESS_WARNINGS_ANNOTATION = "addSuppressWarningsAnnotation";

	/**
	 * Option to suppress generation of the Jakarta Data static metamodel,
	 * even when Jakarta Data is available on the build path.
	 */
	public static final String SUPPRESS_JAKARTA_DATA_METAMODEL = "suppressJakartaDataMetamodel";

	private static final boolean ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS = false;

	private Context context;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnvironment) {
		super.init( processingEnvironment );
		context = new Context( processingEnvironment );
		context.logMessage(
				Diagnostic.Kind.NOTE,
				"Hibernate compile-time tooling " + Version.getVersionString()
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
		final PackageElement jakartaTransactionsPackage =
				context.getProcessingEnvironment().getElementUtils()
						.getPackageElement( "jakarta.transactions" );
		final PackageElement jakartaDataPackage =
				context.getProcessingEnvironment().getElementUtils()
						.getPackageElement( "jakarta.data" );
		final PackageElement quarkusOrmPackage =
				context.getProcessingEnvironment().getElementUtils()
						.getPackageElement( "io.quarkus.hibernate.orm" );

		PackageElement quarkusOrmPanachePackage =
				context.getProcessingEnvironment().getElementUtils()
						.getPackageElement( "io.quarkus.hibernate.orm.panache" );
		PackageElement quarkusReactivePanachePackage =
				context.getProcessingEnvironment().getElementUtils()
						.getPackageElement( "io.quarkus.hibernate.reactive.panache" );
		if ( quarkusReactivePanachePackage != null
				&& quarkusOrmPanachePackage != null ) {
			context.logMessage(
					Diagnostic.Kind.WARNING,
					"Both Quarkus Hibernate ORM and Hibernate Reactive with Panache detected: this is not supported, so will proceed as if none were there"
			);
			quarkusOrmPanachePackage = quarkusReactivePanachePackage = null;
		}
		
		context.setAddInjectAnnotation( jakartaInjectPackage != null );
		context.setAddNonnullAnnotation( jakartaAnnotationPackage != null );
		context.setAddGeneratedAnnotation( jakartaAnnotationPackage != null );
		context.setAddDependentAnnotation( jakartaContextPackage != null );
		context.setAddTransactionScopedAnnotation( jakartaTransactionsPackage != null );
		context.setQuarkusInjection( quarkusOrmPackage != null );
		context.setUsesQuarkusOrm( quarkusOrmPanachePackage != null );
		context.setUsesQuarkusReactive( quarkusReactivePanachePackage != null );

		final Map<String, String> options = environment.getOptions();

		boolean suppressJakartaData = parseBoolean( options.get( SUPPRESS_JAKARTA_DATA_METAMODEL ) );

		context.setGenerateJakartaDataStaticMetamodel( !suppressJakartaData && jakartaDataPackage != null );

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
				final StringBuffer b = new StringBuffer();
				Arrays.stream(e.getStackTrace())
						.forEach(stackTraceElement -> {
							b.append(stackTraceElement);
							b.append('\n');
						});
				final Throwable cause = e.getCause();
				final String message =
						cause != null && cause != e
								? e.getMessage() + " caused by " + cause.getMessage()
								: e.getMessage();
				context.logMessage( Diagnostic.Kind.ERROR, "Error generating JPA metamodel: " + message );
				context.logMessage( Diagnostic.Kind.ERROR, b.toString() );
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
						AnnotationMetaEntity.create( typeElement, context );
				context.addMetaAuxiliary( metaEntity.getQualifiedName(), metaEntity );
				context.removeElementToRedo( elementName );
			}
			catch (ProcessLaterException processLaterException) {
				// leave it there for next time
			}
		}

		for ( Element element : roundEnvironment.getRootElements() ) {
			try {
				if ( isEntityOrEmbeddable( element ) ) {
					context.logMessage( Diagnostic.Kind.OTHER, "Processing annotated entity class '" + element + "'" );
					handleRootElementAnnotationMirrors( element );
				}
				else if ( hasAuxiliaryAnnotations( element ) ) {
					context.logMessage( Diagnostic.Kind.OTHER, "Processing annotated class '" + element + "'" );
					handleRootElementAuxiliaryAnnotationMirrors( element );
				}
				else if ( element instanceof TypeElement ) {
					final TypeElement typeElement = (TypeElement) element;
					final AnnotationMirror repository = getAnnotationMirror( element, JD_REPOSITORY );
					if ( repository != null ) {
						final String provider = (String) getAnnotationValue( repository, "provider" );
						if ( provider == null || provider.isEmpty()
								|| provider.equalsIgnoreCase("hibernate") ) {
							context.logMessage( Diagnostic.Kind.OTHER, "Processing repository class '" + element + "'" );
							final AnnotationMetaEntity metaEntity =
									AnnotationMetaEntity.create( typeElement, context );
							context.addMetaAuxiliary( metaEntity.getQualifiedName(), metaEntity );
						}
					}
					else {
						for ( Element member : typeElement.getEnclosedElements() ) {
							if ( hasAnnotation( member, HQL, SQL, FIND ) ) {
								context.logMessage( Diagnostic.Kind.OTHER, "Processing annotated class '" + element + "'" );
								final AnnotationMetaEntity metaEntity =
										AnnotationMetaEntity.create( typeElement, context );
								context.addMetaAuxiliary( metaEntity.getQualifiedName(), metaEntity );
								break;
							}
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
			if ( !context.isAlreadyGenerated(aux) ) {
				context.logMessage( Diagnostic.Kind.OTHER,
						"Writing metamodel for auxiliary '" + aux + "'" );
				ClassWriter.writeFile( aux, context );
				context.markGenerated(aux);
			}
		}

		for ( Metamodel entity : context.getMetaEntities() ) {
			if ( !context.isAlreadyGenerated(entity) ) {
				context.logMessage( Diagnostic.Kind.OTHER,
						"Writing Jakarta Persistence metamodel for entity '" + entity + "'" );
				ClassWriter.writeFile( entity, context );
				context.markGenerated(entity);
			}
		}

		for ( Metamodel entity : context.getDataMetaEntities() ) {
			if ( !context.isAlreadyGenerated(entity) ) {
				context.logMessage( Diagnostic.Kind.OTHER,
						"Writing Jakarta Data metamodel for entity '" + entity + "'" );
				ClassWriter.writeFile( entity, context );
				context.markGenerated(entity);
			}
		}

		processEmbeddables( context.getMetaEmbeddables() );
		processEmbeddables( context.getDataMetaEmbeddables() );
	}

	/**
	 * We cannot process the delayed classes in any order.
	 * There might be dependencies between them.
	 * We need to process the toplevel classes first.
	 */
	private void processEmbeddables(Collection<Metamodel> models) {
		while ( !models.isEmpty() ) {
			final Set<Metamodel> processed = new HashSet<>();
			final int toProcessCountBeforeLoop = models.size();
			for ( Metamodel metamodel : models ) {
				// see METAGEN-36
				if ( context.isAlreadyGenerated(metamodel) ) {
					processed.add( metamodel );
				}
				else if ( !modelGenerationNeedsToBeDeferred(models, metamodel ) ) {
					context.logMessage(
							Diagnostic.Kind.OTHER,
							"Writing metamodel for embeddable " + metamodel
					);
					ClassWriter.writeFile( metamodel, context );
					context.markGenerated(metamodel);
					processed.add( metamodel );
				}
			}
			models.removeAll( processed );
			if ( models.size() >= toProcessCountBeforeLoop ) {
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
			final ContainsAttributeTypeVisitor visitor =
					new ContainsAttributeTypeVisitor( (TypeElement) element, context );
			for ( Metamodel entity : entities ) {
				if ( !entity.equals( containedEntity ) ) {
					final List<? extends Element> enclosedElements =
							entity.getElement().getEnclosedElements();
					for ( Element subElement : fieldsIn(enclosedElements) ) {
						final TypeMirror mirror = subElement.asType();
						if ( TypeKind.DECLARED == mirror.getKind() ) {
							if ( mirror.accept( visitor, subElement ) ) {
								return true;
							}
						}
					}
					for ( Element subElement : methodsIn(enclosedElements) ) {
						final TypeMirror mirror = subElement.asType();
						if ( TypeKind.DECLARED == mirror.getKind() ) {
							if ( mirror.accept( visitor, subElement ) ) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private static boolean isEntityOrEmbeddable(Element element) {
		return containsAnnotation(
				element,
				ENTITY,
				MAPPED_SUPERCLASS,
				EMBEDDABLE
		);
	}

	private boolean hasAuxiliaryAnnotations(Element element) {
		return containsAnnotation(
				element,
				NAMED_QUERY,
				NAMED_QUERIES,
				NAMED_NATIVE_QUERY,
				NAMED_NATIVE_QUERIES,
				SQL_RESULT_SET_MAPPING,
				SQL_RESULT_SET_MAPPINGS,
				NAMED_ENTITY_GRAPH,
				NAMED_ENTITY_GRAPHS,
				HIB_NAMED_QUERY,
				HIB_NAMED_QUERIES,
				HIB_NAMED_NATIVE_QUERY,
				HIB_NAMED_NATIVE_QUERIES,
				HIB_FETCH_PROFILE,
				HIB_FETCH_PROFILES,
				HIB_FILTER_DEF,
				HIB_FILTER_DEFS
		);
	}

	private void handleRootElementAnnotationMirrors(final Element element) {
		if ( isClassOrRecordType( element ) ) {
			if ( hasAnnotation( element, ENTITY, MAPPED_SUPERCLASS, EMBEDDABLE ) ) {
				final TypeElement typeElement = (TypeElement) element;
				final String qualifiedName = typeElement.getQualifiedName().toString();
				final Metamodel alreadyExistingMetaEntity =
						tryGettingExistingEntityFromContext( typeElement, qualifiedName );
				if ( alreadyExistingMetaEntity != null && alreadyExistingMetaEntity.isMetaComplete() ) {
					context.logMessage(
							Diagnostic.Kind.OTHER,
							"Skipping processing of annotations for '" + qualifiedName
									+ "' since XML configuration is metadata complete.");
				}
				else {
					final boolean requiresLazyMemberInitialization
							= hasAnnotation( element, EMBEDDABLE, MAPPED_SUPERCLASS );
					final AnnotationMetaEntity metaEntity =
							AnnotationMetaEntity.create( typeElement, context,
									requiresLazyMemberInitialization,
									true, false );
					if ( alreadyExistingMetaEntity != null ) {
						metaEntity.mergeInMembers( alreadyExistingMetaEntity );
					}
					addMetamodelToContext( typeElement, metaEntity );
					if ( context.generateJakartaDataStaticMetamodel()
							// Don't generate a Jakarta Data metamodel
							// if this entity was partially mapped in XML
							&& alreadyExistingMetaEntity == null ) {
						final AnnotationMetaEntity dataMetaEntity =
								AnnotationMetaEntity.create( typeElement, context,
										requiresLazyMemberInitialization,
										true, true );
//						final Metamodel alreadyExistingDataMetaEntity =
//								tryGettingExistingDataEntityFromContext( mirror, '_' + qualifiedName );
//						if ( alreadyExistingDataMetaEntity != null ) {
//							dataMetaEntity.mergeInMembers( alreadyExistingDataMetaEntity );
//						}
						addDataMetamodelToContext( typeElement, dataMetaEntity );
					}
				}
			}
		}
	}

	private void handleRootElementAuxiliaryAnnotationMirrors(final Element element) {
		if ( element instanceof TypeElement ) {
			final AnnotationMetaEntity metaEntity =
					AnnotationMetaEntity.create( (TypeElement) element, context );
			context.addMetaAuxiliary( metaEntity.getQualifiedName(), metaEntity );
		}
		else if ( element instanceof PackageElement ) {
			final AnnotationMetaPackage metaEntity =
					AnnotationMetaPackage.create( (PackageElement) element, context );
			context.addMetaAuxiliary( metaEntity.getQualifiedName(), metaEntity );
		}
		//TODO: handle PackageElement
	}

	private @Nullable Metamodel tryGettingExistingEntityFromContext(TypeElement typeElement, String qualifiedName) {
		if ( hasAnnotation( typeElement, ENTITY, MAPPED_SUPERCLASS ) ) {
			return context.getMetaEntity( qualifiedName );
		}
		else if ( hasAnnotation( typeElement, EMBEDDABLE ) ) {
			return context.getMetaEmbeddable( qualifiedName );
		}
		return null;
	}

	private void addMetamodelToContext(TypeElement typeElement, AnnotationMetaEntity entity) {
		final String key = entity.getQualifiedName();
		if ( hasAnnotation( typeElement, ENTITY ) ) {
			context.addMetaEntity( key, entity );
		}
		else if ( hasAnnotation( typeElement, MAPPED_SUPERCLASS ) ) {
			context.addMetaEntity( key, entity );
		}
		else if ( hasAnnotation( typeElement, EMBEDDABLE ) ) {
			context.addMetaEmbeddable( key, entity );
		}
	}

	private void addDataMetamodelToContext(TypeElement typeElement, AnnotationMetaEntity entity) {
		final String key = entity.getQualifiedName();
		if ( hasAnnotation( typeElement, ENTITY ) ) {
			context.addDataMetaEntity( key, entity );
		}
		else if ( hasAnnotation( typeElement, MAPPED_SUPERCLASS ) ) {
			context.addDataMetaEntity( key, entity );
		}
		else if ( hasAnnotation( typeElement, EMBEDDABLE ) ) {
			context.addDataMetaEmbeddable( key, entity );
		}
	}

}
