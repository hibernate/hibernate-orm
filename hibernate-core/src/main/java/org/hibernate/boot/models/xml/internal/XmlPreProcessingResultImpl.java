package org.hibernate.boot.models.xml.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.boot.models.xml.spi.XmlDocument;
import org.hibernate.boot.models.xml.spi.XmlPreProcessingResult;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class XmlPreProcessingResultImpl implements XmlPreProcessingResult {
	private final PersistenceUnitMetadataImpl persistenceUnitMetadata;
	private final List<XmlDocument> documents = new ArrayList<>();
	private final List<String> managedClasses = new ArrayList<>();
	private final List<String> managedNames = new ArrayList<>();

	public XmlPreProcessingResultImpl(PersistenceUnitMetadataImpl persistenceUnitMetadata) {
		this.persistenceUnitMetadata = persistenceUnitMetadata;
	}

	public XmlPreProcessingResultImpl(PersistenceUnitMetadata persistenceUnitMetadata) {
		this( (PersistenceUnitMetadataImpl) persistenceUnitMetadata );
	}

	/**
	 * Intended for testing
	 */
	public XmlPreProcessingResultImpl() {
		this( new PersistenceUnitMetadataImpl() );
	}

	public PersistenceUnitMetadataImpl getPersistenceUnitMetadata() {
		return persistenceUnitMetadata;
	}

	@Override
	public List<XmlDocument> getDocuments() {
		return documents;
	}

	@Override
	public List<String> getMappedClasses() {
		return managedClasses;
	}

	@Override
	public List<String> getMappedNames() {
		return managedNames;
	}

	public void addDocument(Binding<JaxbEntityMappingsImpl> binding) {
		final XmlDocumentImpl xmlDocument = XmlDocumentImpl.consume( binding, persistenceUnitMetadata );
		documents.add( xmlDocument );

		final JaxbEntityMappingsImpl jaxbRoot = binding.getRoot();
		persistenceUnitMetadata.apply( jaxbRoot.getPersistenceUnitMetadata() );
		jaxbRoot.getEmbeddables().forEach( (jaxbEmbeddable) -> {
			if ( StringHelper.isNotEmpty( jaxbEmbeddable.getClazz() ) ) {
				managedClasses.add( XmlProcessingHelper.determineClassName( jaxbRoot, jaxbEmbeddable ) );
			}
			else if ( StringHelper.isNotEmpty( jaxbEmbeddable.getName() ) ) {
				managedNames.add( jaxbEmbeddable.getName() );
			}
		} );
		jaxbRoot.getMappedSuperclasses().forEach( (jaxbMappedSuperclass) -> {
			managedClasses.add( XmlProcessingHelper.determineClassName( jaxbRoot, jaxbMappedSuperclass ) );
		} );
		final Map<String, String> entityNamesByClass = new HashMap<>();
		jaxbRoot.getEntities().forEach( (jaxbEntity) -> {
			if ( StringHelper.isNotEmpty( jaxbEntity.getClazz() ) ) {
				final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbEntity );
				final String entityName = jaxbEntity.getName();
				if ( entityName != null && !entityName.isBlank() ) {
					final String previousName = entityNamesByClass.putIfAbsent( className, entityName );
					if ( previousName != null && !previousName.equals( entityName ) ) {
						throw new MappingException(
								"Entity class '" + className + "' is mapped in the same document"
										+ " with different entity names '" + previousName + "' and '"
										+ entityName + "'",
								binding.getOrigin()
						);
					}
				}
				managedClasses.add( className );
			}
			else if ( StringHelper.isNotEmpty( jaxbEntity.getName() ) ) {
				managedNames.add( jaxbEntity.getName() );
			}
		} );
	}
}
