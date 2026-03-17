/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.scanning;

import jakarta.persistence.FetchType;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl;
import org.hibernate.boot.jaxb.internal.ConfigurationBinder;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.scan.jandex.IndexBuildingScanner;

import javax.sql.DataSource;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.internal.util.collections.CollectionHelper.combine;
import static org.hibernate.orm.test.bootstrap.scanning.ScanningContextTestingImpl.SCANNING_CONTEXT;

/**
 * @author Steve Ebersole
 */
public class ScannedPersistenceUnitInfo implements PersistenceUnitInfo {
	private final ParsedPersistenceXmlDescriptor descriptor;
	private final ClassLoader unitClassLoader;
	private final Collection<String> discoveredClasses;
	private final List<String> mappingFiles;

	public ScannedPersistenceUnitInfo(
			ParsedPersistenceXmlDescriptor descriptor,
			ClassLoader unitClassLoader,
			ScanningResult scanningResult) {
		this.descriptor = descriptor;
		this.unitClassLoader = unitClassLoader;
		this.discoveredClasses = discoveredClassNames( scanningResult );
		this.mappingFiles = combineMappingFiles( descriptor.getMappingFileNames(), scanningResult.mappingFiles() );
	}

	private static List<String> combineMappingFiles(List<String> mappingFileNames, Set<URI> uris) {
		final List<String> results = CollectionHelper.arrayList(
				CollectionHelper.size( mappingFileNames ) + CollectionHelper.size( uris )
		);

		if ( mappingFileNames != null ) {
			results.addAll( mappingFileNames );
		}

		if ( uris != null ) {
			uris.forEach( (uri) -> results.add( uri.toString() ) );
		}

		return  results;
	}

	private static Collection<String> discoveredClassNames(ScanningResult scanningResult) {
		final List<String> names = arrayList( CollectionHelper.size( scanningResult.discoveredClasses() ) + CollectionHelper.size( scanningResult.discoveredPackages() ) );
		if ( CollectionHelper.isNotEmpty( scanningResult.discoveredClasses() ) ) {
			names.addAll( scanningResult.discoveredClasses() );
		}
		if ( CollectionHelper.isNotEmpty( scanningResult.discoveredPackages() ) ) {
//			names.addAll( scanningResult.getDiscoveredPackages() );
			scanningResult.discoveredPackages().forEach( (packageName) -> names.add( packageName + ".package-info" ) );
		}
		return names;
	}

	@Override
	public String getPersistenceUnitName() {
		return descriptor.getName();
	}

	@Override
	public String getPersistenceProviderClassName() {
		return descriptor.getProviderClassName();
	}

	@Override
	public String getScopeAnnotationName() {
		// todo (jap4) : need to add these new methods to ParsedPersistenceXmlDescriptor from JAXB model
		return null;
	}

	@Override
	public List<String> getQualifierAnnotationNames() {
		// todo (jap4) : need to add these new methods to ParsedPersistenceXmlDescriptor from JAXB model
		return List.of();
	}

	@Override
	public PersistenceUnitTransactionType getTransactionType() {
		return descriptor.getPersistenceUnitTransactionType();
	}

	@Override
	public DataSource getJtaDataSource() {
		// DataSources are not used in the tests
		return null;
	}

	@Override
	public DataSource getNonJtaDataSource() {
		// DataSources are not used in the tests
		return null;
	}

	@Override
	public List<String> getMappingFileNames() {
		return mappingFiles;
	}

	@Override
	public List<URL> getJarFileUrls() {
		return descriptor.getJarFileUrls();
	}

	@Override
	public URL getPersistenceUnitRootUrl() {
		return descriptor.getPersistenceUnitRootUrl();
	}

	@Override
	public List<String> getManagedClassNames() {
		return descriptor.getManagedClassNames();
	}

	@Override
	public List<String> getAllManagedClassNames() {
		return combine( getManagedClassNames(), discoveredClasses );
	}

	@Override
	public boolean excludeUnlistedClasses() {
		return descriptor.isExcludeUnlistedClasses();
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return descriptor.getSharedCacheMode();
	}

	@Override
	public ValidationMode getValidationMode() {
		return descriptor.getValidationMode();
	}

	@Override
	public FetchType getDefaultToOneFetchType() {
		return descriptor.getDefaultToOneFetchType();
	}

	@Override
	public Properties getProperties() {
		return descriptor.getProperties();
	}

	@Override
	public String getPersistenceXMLSchemaVersion() {
		// todo (jap4) : need to add these new methods to ParsedPersistenceXmlDescriptor from JAXB model
		return "8.0";
	}

	@Override
	public ClassLoader getClassLoader() {
		return unitClassLoader;
	}

	@Override
	public void addTransformer(ClassTransformer transformer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ClassLoader getNewTempClassLoader() {
		return null;
	}

	public static ScannedPersistenceUnitInfo create(URL unitRootUrl, String unitName) {
		final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

		var rootArchive = SCANNING_CONTEXT.getArchiveDescriptorFactory().buildArchiveDescriptor( unitRootUrl );
		var persistenceXmlEntry = rootArchive.findEntry( "META-INF/persistence.xml" );
		if ( persistenceXmlEntry == null ) {
			throw new RuntimeException( "Unable to locate persistence.xml at " + unitRootUrl );
		}

		try (InputStream stream = persistenceXmlEntry.getStreamAccess().accessInputStream()) {
			var xmlBinding = new ConfigurationBinder( null ).bind(
					stream,
					new Origin( SourceType.INPUT_STREAM, "META-INF/persistence.xml" )
			);

			var jaxbPersistence = xmlBinding.getRoot();
			var jaxbPersistenceUnit = findNamedUnit( jaxbPersistence, unitName );

			// this will test the form building the Jandex index
			var scanner = new IndexBuildingScanner( SCANNING_CONTEXT );
			var scanningResult = scanner.jpaScan( rootArchive, jaxbPersistenceUnit );

			var descriptor = new ParsedPersistenceXmlDescriptor(unitRootUrl);
			descriptor.setName( jaxbPersistenceUnit.getName() );
			descriptor.setProviderClassName( jaxbPersistenceUnit.getProvider() );
			descriptor.setTransactionType( jaxbPersistenceUnit.getTransactionType() );
			// for tests using this class, this is always true
			descriptor.setExcludeUnlistedClasses( true );
			descriptor.setUseQuotedIdentifiers( jaxbPersistenceUnit.isExcludeUnlistedClasses() == Boolean.TRUE );
			descriptor.setSharedCacheMode( jaxbPersistenceUnit.getSharedCacheMode() );
			descriptor.setValidationMode( jaxbPersistenceUnit.getValidationMode() );
			descriptor.addClasses( jaxbPersistenceUnit.getClasses() );
			descriptor.addMappingFiles( jaxbPersistenceUnit.getMappingFiles() );
			descriptor.getProperties().putAll( extractProperties( jaxbPersistenceUnit.getPropertyContainer() ) );

			return new ScannedPersistenceUnitInfo( descriptor, contextClassLoader, scanningResult );
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	private static JaxbPersistenceUnitImpl findNamedUnit(JaxbPersistenceImpl jaxbPersistence, String unitName) {
		for ( JaxbPersistenceUnitImpl jaxbPersistenceUnit : jaxbPersistence.getPersistenceUnit() ) {
			if ( jaxbPersistenceUnit.getName().equals( unitName ) ) {
				return jaxbPersistenceUnit;
			}
		}
		throw new RuntimeException( "Could not find JaxbPersistenceUnit with name " + unitName );
	}


	private static Map<?, ?> extractProperties(JaxbPersistenceUnitImpl.JaxbPropertiesImpl propertyContainer) {
		var map = new HashMap<>();
		propertyContainer.getProperties().forEach( jaxbProperty -> {
			map.put( jaxbProperty.getName(), jaxbProperty.getValue() );
		} );
		return map;

	}
}
