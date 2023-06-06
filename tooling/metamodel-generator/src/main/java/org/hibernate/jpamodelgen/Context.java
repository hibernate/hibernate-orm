/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.model.MetaEntity;
import org.hibernate.jpamodelgen.util.AccessType;
import org.hibernate.jpamodelgen.util.AccessTypeInformation;
import org.hibernate.jpamodelgen.util.Constants;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public final class Context {
	private static final String DEFAULT_PERSISTENCE_XML_LOCATION = "/META-INF/persistence.xml";

	/**
	 * Used for keeping track of parsed entities and mapped super classes (xml + annotations).
	 */
	private final Map<String, MetaEntity> metaEntities = new HashMap<>();

	/**
	 * Used for keeping track of parsed embeddable entities. These entities have to be kept separate since
	 * they are lazily initialized.
	 */
	private final Map<String, MetaEntity> metaEmbeddables = new HashMap<>();

	private final Map<String, MetaEntity> metaAuxiliaries = new HashMap<>();

	private final Map<String, AccessTypeInformation> accessTypeInformation = new HashMap<>();

	private final ProcessingEnvironment pe;
	private final boolean logDebug;
	private final boolean lazyXmlParsing;
	private final String persistenceXmlLocation;
	private final List<String> ormXmlFiles;
	private final String generatedAnnotation;

	/**
	 * Whether all mapping files are xml-mapping-metadata-complete. In this case no annotation processing will take
	 * place.
	 */
	private Boolean fullyXmlConfigured;
	private boolean addGeneratedAnnotation = true;
	private boolean addGenerationDate;
	private boolean addSuppressWarningsAnnotation;
	private AccessType persistenceUnitDefaultAccessType;

	// keep track of all classes for which model have been generated
	private final Collection<String> generatedModelClasses = new HashSet<String>();

	public Context(ProcessingEnvironment pe) {
		this.pe = pe;

		String persistenceXmlOption = pe.getOptions().get( JPAMetaModelEntityProcessor.PERSISTENCE_XML_OPTION );
		if ( persistenceXmlOption != null ) {
			if ( !persistenceXmlOption.startsWith( Constants.PATH_SEPARATOR ) ) {
				persistenceXmlOption = Constants.PATH_SEPARATOR + persistenceXmlOption;
			}
			persistenceXmlLocation = persistenceXmlOption;
		}
		else {
			persistenceXmlLocation = DEFAULT_PERSISTENCE_XML_LOCATION;
		}

		String ormXmlOption = pe.getOptions().get( JPAMetaModelEntityProcessor.ORM_XML_OPTION );
		if ( ormXmlOption != null ) {
			ormXmlFiles = new ArrayList<>();
			for ( String ormFile : ormXmlOption.split( "," ) ) {
				if ( !ormFile.startsWith( Constants.PATH_SEPARATOR ) ) {
					ormFile = Constants.PATH_SEPARATOR + ormFile;
				}
				ormXmlFiles.add( ormFile );
			}
		}
		else {
			ormXmlFiles = Collections.emptyList();
		}

		lazyXmlParsing = Boolean.parseBoolean( pe.getOptions().get( JPAMetaModelEntityProcessor.LAZY_XML_PARSING ) );
		logDebug = Boolean.parseBoolean( pe.getOptions().get( JPAMetaModelEntityProcessor.DEBUG_OPTION ) );

		// Workaround that Eclipse transformer tries to replace this constant which we don't want
		String j = "j";
		TypeElement java8AndBelowGeneratedAnnotation =
				pe.getElementUtils().getTypeElement( j + "avax.annotation.Generated" );
		if ( java8AndBelowGeneratedAnnotation != null ) {
			generatedAnnotation = java8AndBelowGeneratedAnnotation.getQualifiedName().toString();
		}
		else {
			// Using the new name for this annotation in Java 9 and above
			generatedAnnotation = "javax.annotation.processing.Generated";
		}
	}

	public ProcessingEnvironment getProcessingEnvironment() {
		return pe;
	}

	public boolean addGeneratedAnnotation() {
		return addGeneratedAnnotation;
	}

	public String getGeneratedAnnotationFqcn() {
		return generatedAnnotation;
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
		return pe.getElementUtils();
	}

	public Types getTypeUtils() {
		return pe.getTypeUtils();
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

	public @Nullable MetaEntity getMetaEntity(String fqcn) {
		return metaEntities.get( fqcn );
	}

	public Collection<MetaEntity> getMetaEntities() {
		return metaEntities.values();
	}

	public void addMetaEntity(String fqcn, MetaEntity metaEntity) {
		metaEntities.put( fqcn, metaEntity );
	}

	public boolean containsMetaEmbeddable(String fqcn) {
		return metaEmbeddables.containsKey( fqcn );
	}

	public @Nullable MetaEntity getMetaEmbeddable(String fqcn) {
		return metaEmbeddables.get( fqcn );
	}

	public void addMetaEmbeddable(String fqcn, MetaEntity metaEntity) {
		metaEmbeddables.put( fqcn, metaEntity );
	}

	public Collection<MetaEntity> getMetaEmbeddables() {
		return metaEmbeddables.values();
	}

	public @Nullable MetaEntity getMetaAuxiliary(String fqcn) {
		return metaAuxiliaries.get( fqcn );
	}

	public Collection<MetaEntity> getMetaAuxiliaries() {
		return metaAuxiliaries.values();
	}

	public void addMetaAuxiliary(String fqcn, MetaEntity metaEntity) {
		metaAuxiliaries.put( fqcn, metaEntity );
	}

	public void addAccessTypeInformation(String fqcn, AccessTypeInformation info) {
		accessTypeInformation.put( fqcn, info );
	}

	public @Nullable AccessTypeInformation getAccessTypeInfo(String fqcn) {
		return accessTypeInformation.get( fqcn );
	}

	public TypeElement getTypeElementForFullyQualifiedName(String fqcn) {
		Elements elementUtils = pe.getElementUtils();
		return elementUtils.getTypeElement( fqcn );
	}

	void markGenerated(String name) {
		generatedModelClasses.add( name );
	}

	boolean isAlreadyGenerated(String name) {
		return generatedModelClasses.contains( name );
	}

	public void logMessage(Diagnostic.Kind type, String message) {
		if ( !logDebug && type.equals( Diagnostic.Kind.OTHER ) ) {
			return;
		}
		pe.getMessager().printMessage( type, message );
	}

	public boolean isFullyXmlConfigured() {
		return fullyXmlConfigured != null && fullyXmlConfigured.booleanValue();
	}

	public void mappingDocumentFullyXmlConfigured(boolean fullyXmlConfigured) {
		if ( this.fullyXmlConfigured == null ) {
			this.fullyXmlConfigured = fullyXmlConfigured;
		}
		else {
			this.fullyXmlConfigured = this.fullyXmlConfigured && fullyXmlConfigured;
		}
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
}
