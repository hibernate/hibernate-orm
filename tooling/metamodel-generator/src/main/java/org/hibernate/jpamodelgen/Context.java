/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.model.Metamodel;
import org.hibernate.jpamodelgen.util.AccessType;
import org.hibernate.jpamodelgen.util.AccessTypeInformation;

import org.checkerframework.checker.nullness.qual.Nullable;

import static java.lang.Boolean.parseBoolean;
import static java.util.Collections.emptyList;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public final class Context {
	private static final String DEFAULT_PERSISTENCE_XML_LOCATION = "/META-INF/persistence.xml";

	/**
	 * Used for keeping track of parsed entities and mapped super classes (XML + annotations).
	 */
	private final Map<String, Metamodel> metaEntities = new HashMap<>();

	/**
	 * Used for keeping track of parsed embeddable entities. These entities have to be kept separate since
	 * they are lazily initialized.
	 */
	private final Map<String, Metamodel> metaEmbeddables = new HashMap<>();

	private final Map<String, Metamodel> metaAuxiliaries = new HashMap<>();

	private final Map<String, AccessTypeInformation> accessTypeInformation = new HashMap<>();

	private final Set<CharSequence> elementsToRedo = new HashSet<>();

	private final ProcessingEnvironment processingEnvironment;
	private final boolean logDebug;
	private final boolean lazyXmlParsing;
	private final String persistenceXmlLocation;
	private final List<String> ormXmlFiles;

	/**
	 * Whether all mapping files are xml-mapping-metadata-complete. In this case no annotation processing will take
	 * place.
	 */
	private Boolean fullyXmlConfigured;
	private boolean addInjectAnnotation = false;
	private boolean addDependentAnnotation = false;
	private boolean addNonnullAnnotation = false;
	private boolean addGeneratedAnnotation = true;
	private boolean addGenerationDate;
	private boolean addSuppressWarningsAnnotation;
	private AccessType persistenceUnitDefaultAccessType;

	// keep track of all classes for which model have been generated
	private final Collection<String> generatedModelClasses = new HashSet<>();

	// keep track of which named queries have been checked
	private final Set<String> checkedNamedQueries = new HashSet<>();

	public Context(ProcessingEnvironment processingEnvironment) {
		this.processingEnvironment = processingEnvironment;

		final Map<String, String> options = processingEnvironment.getOptions();

		String persistenceXmlOption = options.get( JPAMetaModelEntityProcessor.PERSISTENCE_XML_OPTION );
		if ( persistenceXmlOption != null ) {
			if ( !persistenceXmlOption.startsWith("/") ) {
				persistenceXmlOption = "/" + persistenceXmlOption;
			}
			persistenceXmlLocation = persistenceXmlOption;
		}
		else {
			persistenceXmlLocation = DEFAULT_PERSISTENCE_XML_LOCATION;
		}

		String ormXmlOption = options.get( JPAMetaModelEntityProcessor.ORM_XML_OPTION );
		if ( ormXmlOption != null ) {
			ormXmlFiles = new ArrayList<>();
			for ( String ormFile : ormXmlOption.split( "," ) ) {
				if ( !ormFile.startsWith("/") ) {
					ormFile = "/" + ormFile;
				}
				ormXmlFiles.add( ormFile );
			}
		}
		else {
			ormXmlFiles = emptyList();
		}

		lazyXmlParsing = parseBoolean( options.get( JPAMetaModelEntityProcessor.LAZY_XML_PARSING ) );
		logDebug = parseBoolean( options.get( JPAMetaModelEntityProcessor.DEBUG_OPTION ) );
	}

	public ProcessingEnvironment getProcessingEnvironment() {
		return processingEnvironment;
	}

	public boolean addInjectAnnotation() {
		return addInjectAnnotation;
	}

	public void setAddInjectAnnotation(boolean addInjectAnnotation) {
		this.addInjectAnnotation = addInjectAnnotation;
	}

	public boolean addDependentAnnotation() {
		return addDependentAnnotation;
	}

	public void setAddDependentAnnotation(boolean addDependentAnnotation) {
		this.addDependentAnnotation = addDependentAnnotation;
	}

	public boolean addNonnullAnnotation() {
		return addNonnullAnnotation;
	}

	public void setAddNonnullAnnotation(boolean addNonnullAnnotation) {
		this.addNonnullAnnotation = addNonnullAnnotation;
	}

	public boolean addGeneratedAnnotation() {
		return addGeneratedAnnotation;
	}

	public void setAddGeneratedAnnotation(boolean addGeneratedAnnotation) {
		this.addGeneratedAnnotation = addGeneratedAnnotation;
	}

	public boolean addGeneratedDate() {
		return addGenerationDate;
	}

	public void setAddGenerationDate(boolean addGenerationDate) {
		this.addGenerationDate = addGenerationDate;
	}

	public boolean isAddSuppressWarningsAnnotation() {
		return addSuppressWarningsAnnotation;
	}

	public void setAddSuppressWarningsAnnotation(boolean addSuppressWarningsAnnotation) {
		this.addSuppressWarningsAnnotation = addSuppressWarningsAnnotation;
	}

	public Elements getElementUtils() {
		return processingEnvironment.getElementUtils();
	}

	public Types getTypeUtils() {
		return processingEnvironment.getTypeUtils();
	}

	public String getPersistenceXmlLocation() {
		return persistenceXmlLocation;
	}

	public List<String> getOrmXmlFiles() {
		return ormXmlFiles;
	}

	public boolean containsMetaEntity(String fqcn) {
		return metaEntities.containsKey( fqcn );
	}

	public @Nullable Metamodel getMetaEntity(String fqcn) {
		return metaEntities.get( fqcn );
	}

	public Collection<Metamodel> getMetaEntities() {
		return metaEntities.values();
	}

	public void addMetaEntity(String fqcn, Metamodel metaEntity) {
		metaEntities.put( fqcn, metaEntity );
	}

	public boolean containsMetaEmbeddable(String fqcn) {
		return metaEmbeddables.containsKey( fqcn );
	}

	public @Nullable Metamodel getMetaEmbeddable(String fqcn) {
		return metaEmbeddables.get( fqcn );
	}

	public void addMetaEmbeddable(String fqcn, Metamodel metaEntity) {
		metaEmbeddables.put( fqcn, metaEntity );
	}

	public Collection<Metamodel> getMetaEmbeddables() {
		return metaEmbeddables.values();
	}

	public @Nullable Metamodel getMetaAuxiliary(String fqcn) {
		return metaAuxiliaries.get( fqcn );
	}

	public Collection<Metamodel> getMetaAuxiliaries() {
		return metaAuxiliaries.values();
	}

	public void addMetaAuxiliary(String fqcn, Metamodel metamodel) {
		metaAuxiliaries.put( fqcn, metamodel);
	}

	public void addAccessTypeInformation(String fqcn, AccessTypeInformation info) {
		accessTypeInformation.put( fqcn, info );
	}

	public @Nullable AccessTypeInformation getAccessTypeInfo(String fqcn) {
		return accessTypeInformation.get( fqcn );
	}

	public TypeElement getTypeElementForFullyQualifiedName(String fqcn) {
		Elements elementUtils = processingEnvironment.getElementUtils();
		return elementUtils.getTypeElement( fqcn );
	}

	void markGenerated(String name) {
		generatedModelClasses.add( name );
	}

	boolean isAlreadyGenerated(String name) {
		return generatedModelClasses.contains( name );
	}

	public Set<CharSequence> getElementsToRedo() {
		return elementsToRedo;
	}

	public void addElementToRedo(CharSequence qualifiedName) {
		elementsToRedo.add( qualifiedName );
	}

	public void removeElementToRedo(CharSequence qualifiedName) {
		elementsToRedo.remove( qualifiedName );
	}

	public void logMessage(Diagnostic.Kind type, String message) {
		if ( logDebug || type != Diagnostic.Kind.OTHER ) {
			processingEnvironment.getMessager().printMessage( type, message );
		}
	}

	public boolean isFullyXmlConfigured() {
		return fullyXmlConfigured != null && fullyXmlConfigured;
	}

	public void mappingDocumentFullyXmlConfigured(boolean fullyXmlConfigured) {
		this.fullyXmlConfigured = this.fullyXmlConfigured == null
				? fullyXmlConfigured
				: this.fullyXmlConfigured && fullyXmlConfigured;
	}

	public AccessType getPersistenceUnitDefaultAccessType() {
		return persistenceUnitDefaultAccessType;
	}

	public void setPersistenceUnitDefaultAccessType(AccessType persistenceUnitDefaultAccessType) {
		this.persistenceUnitDefaultAccessType = persistenceUnitDefaultAccessType;
	}

	public boolean doLazyXmlParsing() {
		return lazyXmlParsing;
	}

	public void message(Element method, String message, Diagnostic.Kind severity) {
		getProcessingEnvironment().getMessager()
				.printMessage( severity, message, method );
	}

	public void message(Element method, AnnotationMirror mirror, AnnotationValue value, String message, Diagnostic.Kind severity) {
		getProcessingEnvironment().getMessager()
				.printMessage( severity, message, method, mirror, value );
	}

	public void message(Element method, AnnotationMirror mirror, String message, Diagnostic.Kind severity) {
		getProcessingEnvironment().getMessager()
				.printMessage( severity, message, method, mirror );
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "Context" );
		sb.append( "{accessTypeInformation=" ).append( accessTypeInformation );
		sb.append( ", logDebug=" ).append( logDebug );
		sb.append( ", lazyXmlParsing=" ).append( lazyXmlParsing );
		sb.append( ", fullyXmlConfigured=" ).append( fullyXmlConfigured );
		sb.append( ", ormXmlFiles=" ).append( ormXmlFiles );
		sb.append( ", persistenceXmlLocation='" ).append( persistenceXmlLocation ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}

	public boolean checkNamedQuery(String name) {
		return checkedNamedQueries.add(name);
	}
}
