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
import org.hibernate.processor.util.Constants;
import org.hibernate.processor.xml.JpaDescriptorParser;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
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
import static org.hibernate.processor.HibernateProcessor.EXCLUDE;
import static org.hibernate.processor.HibernateProcessor.FULLY_ANNOTATION_CONFIGURED_OPTION;
import static org.hibernate.processor.HibernateProcessor.INCLUDE;
import static org.hibernate.processor.HibernateProcessor.LAZY_XML_PARSING;
import static org.hibernate.processor.HibernateProcessor.ORM_XML_OPTION;
import static org.hibernate.processor.HibernateProcessor.PERSISTENCE_XML_OPTION;
import static org.hibernate.processor.HibernateProcessor.SUPPRESS_JAKARTA_DATA_METAMODEL;
import static org.hibernate.processor.util.Constants.EMBEDDABLE;
import static org.hibernate.processor.util.Constants.ENTITY;
import static org.hibernate.processor.util.Constants.FIND;
import static org.hibernate.processor.util.Constants.HIB_FETCH_PROFILE;
import static org.hibernate.processor.util.Constants.HIB_FETCH_PROFILES;
import static org.hibernate.processor.util.Constants.HIB_FILTER_DEF;
import static org.hibernate.processor.util.Constants.HIB_FILTER_DEFS;
import static org.hibernate.processor.util.Constants.HIB_NAMED_NATIVE_QUERIES;
import static org.hibernate.processor.util.Constants.HIB_NAMED_NATIVE_QUERY;
import static org.hibernate.processor.util.Constants.HIB_NAMED_QUERIES;
import static org.hibernate.processor.util.Constants.HIB_NAMED_QUERY;
import static org.hibernate.processor.util.Constants.HQL;
import static org.hibernate.processor.util.Constants.JD_REPOSITORY;
import static org.hibernate.processor.util.Constants.MAPPED_SUPERCLASS;
import static org.hibernate.processor.util.Constants.NAMED_ENTITY_GRAPH;
import static org.hibernate.processor.util.Constants.NAMED_ENTITY_GRAPHS;
import static org.hibernate.processor.util.Constants.NAMED_NATIVE_QUERIES;
import static org.hibernate.processor.util.Constants.NAMED_NATIVE_QUERY;
import static org.hibernate.processor.util.Constants.NAMED_QUERIES;
import static org.hibernate.processor.util.Constants.NAMED_QUERY;
import static org.hibernate.processor.util.Constants.SQL;
import static org.hibernate.processor.util.Constants.SQL_RESULT_SET_MAPPING;
import static org.hibernate.processor.util.Constants.SQL_RESULT_SET_MAPPINGS;
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
		NAMED_QUERY, NAMED_QUERIES, NAMED_NATIVE_QUERY, NAMED_NATIVE_QUERIES,
		NAMED_ENTITY_GRAPH, NAMED_ENTITY_GRAPHS, SQL_RESULT_SET_MAPPING, SQL_RESULT_SET_MAPPINGS,
		// extra for Hibernate
		HIB_FETCH_PROFILE, HIB_FETCH_PROFILES, HIB_FILTER_DEF, HIB_FILTER_DEFS,
		HIB_NAMED_QUERY, HIB_NAMED_QUERIES, HIB_NAMED_NATIVE_QUERY, HIB_NAMED_NATIVE_QUERIES,
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
		SUPPRESS_JAKARTA_DATA_METAMODEL,
		INCLUDE, EXCLUDE
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
	 * A comma-separated list of warnings to suppress, or simply {@code true}
	 * if {@code @SuppressWarnings({"deprecation","rawtypes"})} should be
	 * added to the generated classes.
	 */
	public static final String ADD_SUPPRESS_WARNINGS_ANNOTATION = "addSuppressWarningsAnnotation";

	/**
	 * Option to suppress generation of the Jakarta Data static metamodel,
	 * even when Jakarta Data is available on the build path.
	 */
	public static final String SUPPRESS_JAKARTA_DATA_METAMODEL = "suppressJakartaDataMetamodel";

	/**
	 * Option to include only certain types, according to a list of patterns.
	 * The wildcard character is {@code *}, and patterns are comma-separated.
	 * For example: {@code *.entity.*,*Repository}. The default include is
	 * simply {@code *}, meaning that all types are included.
	 */
	public static final String INCLUDE = "include";

	/**
	 * Option to exclude certain types, according to a list of patterns.
	 * The wildcard character is {@code *}, and patterns are comma-separated.
	 * For example: {@code *.framework.*,*$$}. The default exclude is
	 * empty.
	 */
	public static final String EXCLUDE = "exclude";

	private static final boolean ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS = false;

	public static final String ENTITY_INDEX = "entity.index";

	private Context context;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnvironment) {
		super.init( processingEnvironment );
		context = new Context( processingEnvironment );
		context.logMessage(
				Diagnostic.Kind.NOTE,
				"Hibernate compile-time tooling " + Version.getVersionString()
		);

		final boolean fullyAnnotationConfigured = handleSettings( processingEnvironment );
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
		final PackageElement jakartaTransactionPackage =
				context.getProcessingEnvironment().getElementUtils()
						.getPackageElement( "jakarta.transaction" );
		final PackageElement jakartaDataPackage =
				context.getProcessingEnvironment().getElementUtils()
						.getPackageElement( "jakarta.data" );
		final PackageElement quarkusOrmPackage =
				context.getProcessingEnvironment().getElementUtils()
						.getPackageElement( "io.quarkus.hibernate.orm" );
		final PackageElement quarkusReactivePackage =
				context.getProcessingEnvironment().getElementUtils()
						.getPackageElement( "io.quarkus.hibernate.reactive.runtime" );

		PackageElement quarkusOrmPanachePackage =
				context.getProcessingEnvironment().getElementUtils()
						.getPackageElement( "io.quarkus.hibernate.orm.panache" );
		PackageElement quarkusReactivePanachePackage =
				context.getProcessingEnvironment().getElementUtils()
						.getPackageElement( "io.quarkus.hibernate.reactive.panache" );

		if ( packagePresent(quarkusReactivePanachePackage)
				&& packagePresent(quarkusOrmPanachePackage) ) {
			context.logMessage(
					Diagnostic.Kind.WARNING,
					"Both Quarkus Hibernate ORM and Hibernate Reactive with Panache detected: this is not supported, so will proceed as if none were there"
			);
			quarkusOrmPanachePackage = quarkusReactivePanachePackage = null;
		}

		context.setAddInjectAnnotation( packagePresent(jakartaInjectPackage) );
		context.setAddNonnullAnnotation( packagePresent(jakartaAnnotationPackage) );
		context.setAddGeneratedAnnotation( packagePresent(jakartaAnnotationPackage) );
		context.setAddDependentAnnotation( packagePresent(jakartaContextPackage) );
		context.setAddTransactionScopedAnnotation( packagePresent(jakartaTransactionPackage) );
		context.setQuarkusInjection( packagePresent(quarkusOrmPackage) || packagePresent(quarkusReactivePackage) );
		context.setUsesQuarkusOrm( packagePresent(quarkusOrmPanachePackage) );
		context.setUsesQuarkusReactive( packagePresent(quarkusReactivePanachePackage) );

		final Map<String, String> options = environment.getOptions();

		final boolean suppressJakartaData = parseBoolean( options.get( SUPPRESS_JAKARTA_DATA_METAMODEL ) );

		context.setGenerateJakartaDataStaticMetamodel( !suppressJakartaData && packagePresent(jakartaDataPackage) );

		final String setting = options.get( ADD_GENERATED_ANNOTATION );
		if ( setting != null ) {
			context.setAddGeneratedAnnotation( parseBoolean( setting ) );
		}

		context.setAddGenerationDate( parseBoolean( options.get( ADD_GENERATION_DATE ) ) );

		final String suppressedWarnings = options.get( ADD_SUPPRESS_WARNINGS_ANNOTATION );
		if ( suppressedWarnings != null ) {
			if ( parseBoolean(suppressedWarnings) ) {
				// legacy behavior from HHH-12068
				context.setSuppressedWarnings(new String[] {"deprecation", "rawtypes"});
			}
			else {
				context.setSuppressedWarnings( suppressedWarnings.replace(" ","").split(",") );
			}
		}

		context.setInclude( options.getOrDefault( INCLUDE, "*" ) );
		context.setExclude( options.getOrDefault( EXCLUDE, "" ) );

		return parseBoolean( options.get( FULLY_ANNOTATION_CONFIGURED_OPTION ) );
	}

	private static boolean packagePresent(@Nullable PackageElement pack) {
		return pack != null
			//HHH-18019 ecj always returns a non-null PackageElement
			&& !pack.getEnclosedElements().isEmpty();
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
			writeIndex();
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
				final StringWriter stack = new StringWriter();
				e.printStackTrace( new PrintWriter(stack) );
				final Throwable cause = e.getCause();
				final String message =
						cause != null && cause != e
								? e.getMessage() + " caused by " + cause.getMessage()
								: e.getMessage();
				context.logMessage( Diagnostic.Kind.ERROR, "Error running Hibernate processor: " + message );
				context.logMessage( Diagnostic.Kind.ERROR, stack.toString() );
			}
		}
		return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
	}

	private boolean included(Element element) {
		if ( element instanceof TypeElement || element instanceof PackageElement ) {
			final QualifiedNameable nameable = (QualifiedNameable) element;
			return context.isIncluded( nameable.getQualifiedName().toString() );
		}
		else {
			return false;
		}
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
				if ( !included( element )
						|| hasAnnotation( element, Constants.EXCLUDE )
						|| hasPackageAnnotation( element, Constants.EXCLUDE ) ) {
					// skip it completely
				}
				else if ( isEntityOrEmbeddable( element ) ) {
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
						final AnnotationValue provider = getAnnotationValue( repository, "provider" );
						if ( provider == null
								|| provider.getValue().toString().isEmpty()
								|| provider.getValue().toString().equalsIgnoreCase("hibernate") ) {
							context.logMessage( Diagnostic.Kind.OTHER, "Processing repository class '" + element + "'" );
							final AnnotationMetaEntity metaEntity =
									AnnotationMetaEntity.create( typeElement, context );
							if ( metaEntity.isInitialized() ) {
								context.addMetaAuxiliary( metaEntity.getQualifiedName(), metaEntity );
							}
							// otherwise discard it (assume it has query by magical method name stuff)
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

	private boolean hasPackageAnnotation(Element element, String annotation) {
		final PackageElement pack = context.getElementUtils().getPackageOf( element ); // null for module descriptor
		return pack != null && hasAnnotation( pack, annotation );
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
		return hasAnnotation(
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
			if ( isEntityOrEmbeddable( element ) ) {
				final TypeElement typeElement = (TypeElement) element;
				indexEntityName( typeElement );
				indexEnumFields( typeElement );

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
							// no static metamodel for embeddable classes in Jakarta Data
							&& hasAnnotation( element, ENTITY, MAPPED_SUPERCLASS )
							// don't generate a Jakarta Data metamodel
							// if this entity was partially mapped in XML
							&& alreadyExistingMetaEntity == null
							// let a handwritten metamodel "override" the generated one
							// (this is used in the Jakarta Data TCK)
							&& element.getEnclosingElement().getEnclosedElements()
								.stream().noneMatch(e -> e.getSimpleName()
									.contentEquals('_' + element.getSimpleName().toString()))) {
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

	private void indexEntityName(TypeElement typeElement) {
		final AnnotationMirror mirror = getAnnotationMirror( typeElement, ENTITY );
		if ( mirror != null ) {
			context.addEntityNameMapping( entityName( typeElement, mirror ),
					typeElement.getQualifiedName().toString() );
		}
	}

	private static String entityName(TypeElement entityType, AnnotationMirror mirror) {
		final String className = entityType.getSimpleName().toString();
		final AnnotationValue name = getAnnotationValue(mirror, "name" );
		if (name != null) {
			final String explicitName = name.getValue().toString();
			if ( !explicitName.isEmpty() ) {
				return explicitName;
			}
		}
		return className;
	}

	private void indexEnumFields(TypeElement typeElement) {
		for ( Element member : context.getAllMembers(typeElement) ) {
			switch ( member.getKind() ) {
				case FIELD:
					indexEnumValues( member.asType() );
					break;
				case METHOD:
					indexEnumValues( ((ExecutableElement) member).getReturnType() );
					break;
			}
		}
	}

	private void indexEnumValues(TypeMirror type) {
		if ( type.getKind() == TypeKind.DECLARED ) {
			final DeclaredType declaredType = (DeclaredType) type;
			final TypeElement fieldType = (TypeElement) declaredType.asElement();
			if ( fieldType.getKind() == ElementKind.ENUM ) {
				for ( Element enumMember : fieldType.getEnclosedElements() ) {
					if ( enumMember.getKind() == ElementKind.ENUM_CONSTANT ) {
						final Element enclosingElement = fieldType.getEnclosingElement();
						final boolean hasOuterType =
								enclosingElement.getKind().isClass() || enclosingElement.getKind().isInterface();
						context.addEnumValue( fieldType.getQualifiedName().toString(),
								fieldType.getSimpleName().toString(),
								hasOuterType ? ((TypeElement) enclosingElement).getQualifiedName().toString() : null,
								hasOuterType ? enclosingElement.getSimpleName().toString() : null,
								enumMember.getSimpleName().toString() );
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

	private void writeIndex() {
		final ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
		context.getEntityNameMappings().forEach((entityName, className) -> {
			try (Writer writer = processingEnvironment.getFiler()
					.createResource(
							StandardLocation.SOURCE_OUTPUT,
							ENTITY_INDEX,
							entityName,
							processingEnvironment.getElementUtils().getTypeElement( className )
					)
					.openWriter()) {
				writer.append(className);
			}
			catch (IOException e) {
				processingEnvironment.getMessager()
						.printMessage(Diagnostic.Kind.WARNING,
								"could not write entity index " + e.getMessage());
			}
		});
		context.getEnumTypesByValue().forEach((valueName, enumTypeNames) -> {
			try (Writer writer = processingEnvironment.getFiler().createResource(
							StandardLocation.SOURCE_OUTPUT,
							ENTITY_INDEX,
							'.' + valueName,
							processingEnvironment.getElementUtils().getTypeElement( enumTypeNames.iterator().next() )
					)
					.openWriter()) {
				for (String enumTypeName : enumTypeNames) {
					writer.append(enumTypeName).append(" ");
				}
			}
			catch (IOException e) {
				processingEnvironment.getMessager()
						.printMessage(Diagnostic.Kind.WARNING,
								"could not write entity index " + e.getMessage());
			}
		});
	}
}
