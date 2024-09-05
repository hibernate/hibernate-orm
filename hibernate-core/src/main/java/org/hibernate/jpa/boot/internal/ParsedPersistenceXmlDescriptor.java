/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.boot.internal;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import jakarta.persistence.spi.PersistenceUnitInfo;
import org.hibernate.boot.archive.internal.ArchiveHelper;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.spi.ClassTransformer;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.PersistenceUnitTransactionType;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.internal.util.PersistenceUnitTransactionTypeHelper;

import static org.hibernate.boot.archive.internal.ArchiveHelper.getURLFromPath;

/**
 * Describes the information gleaned from a {@code <persistence-unit/>}
 * element in a {@code persistence.xml} file whether parsed directly by
 * Hibernate or passed to us by an EE container as an instance of
 * {@link PersistenceUnitInfo}.
 * <p>
 * Easier to consolidate both views into a single contract and extract
 * information through that shared contract.
 *
 * @author Steve Ebersole
 */
public class ParsedPersistenceXmlDescriptor implements PersistenceUnitDescriptor {
	private final URL persistenceUnitRootUrl;

	private String name;
	private Object nonJtaDataSource;
	private Object jtaDataSource;
	private String providerClassName;
	private PersistenceUnitTransactionType transactionType;
	private boolean useQuotedIdentifiers;
	private boolean excludeUnlistedClasses;
	private ValidationMode validationMode;
	private SharedCacheMode sharedCacheMode;

	private final Properties properties = new Properties();

	private final List<String> classes = new ArrayList<>();
	private final List<String> mappingFiles = new ArrayList<>();
	private final List<URL> jarFileUrls = new ArrayList<>();

	public ParsedPersistenceXmlDescriptor(URL persistenceUnitRootUrl) {
		this.persistenceUnitRootUrl = persistenceUnitRootUrl;
	}

	@Override
	public URL getPersistenceUnitRootUrl() {
		return persistenceUnitRootUrl;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Object getNonJtaDataSource() {
		return nonJtaDataSource;
	}

	public void setNonJtaDataSource(Object nonJtaDataSource) {
		this.nonJtaDataSource = nonJtaDataSource;
	}

	@Override
	public Object getJtaDataSource() {
		return jtaDataSource;
	}

	public void setJtaDataSource(Object jtaDataSource) {
		this.jtaDataSource = jtaDataSource;
	}

	@Override
	public String getProviderClassName() {
		return providerClassName;
	}

	public void setProviderClassName(String providerClassName) {
		this.providerClassName = providerClassName;
	}

	@Override
	public PersistenceUnitTransactionType getPersistenceUnitTransactionType() {
		return transactionType;
	}

	@Override @SuppressWarnings("removal")
	public jakarta.persistence.spi.PersistenceUnitTransactionType getTransactionType() {
		return PersistenceUnitTransactionTypeHelper.toDeprecatedForm( transactionType );
	}

	public void setTransactionType(PersistenceUnitTransactionType transactionType) {
		this.transactionType = transactionType;
	}

	@Override
	public boolean isUseQuotedIdentifiers() {
		return useQuotedIdentifiers;
	}

	public void setUseQuotedIdentifiers(boolean useQuotedIdentifiers) {
		this.useQuotedIdentifiers = useQuotedIdentifiers;
	}

	@Override
	public Properties getProperties() {
		return properties;
	}

	@Override
	public boolean isExcludeUnlistedClasses() {
		return excludeUnlistedClasses;
	}

	public void setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
		this.excludeUnlistedClasses = excludeUnlistedClasses;
	}

	@Override
	public ValidationMode getValidationMode() {
		return validationMode;
	}

	public void setValidationMode(ValidationMode validationMode) {
		this.validationMode = validationMode;
	}

	public void setValidationMode(String validationMode) {
		setValidationMode( ValidationMode.valueOf( validationMode ) );
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return sharedCacheMode;
	}

	public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
		this.sharedCacheMode = sharedCacheMode;
	}

	public void setSharedCacheMode(String sharedCacheMode) {
		setSharedCacheMode( SharedCacheMode.valueOf( sharedCacheMode ) );
	}

	@Override
	public List<String> getManagedClassNames() {
		return classes;
	}

	public void addClasses(String... classes) {
		addClasses( Arrays.asList( classes ) );
	}

	public void addClasses(List<String> classes) {
		this.classes.addAll( classes );
	}

	@Override
	public List<String> getMappingFileNames() {
		return mappingFiles;
	}

	public void addMappingFiles(String... mappingFiles) {
		addMappingFiles( Arrays.asList( mappingFiles ) );
	}

	public void addMappingFiles(List<String> mappingFiles) {
		this.mappingFiles.addAll( mappingFiles );
	}

	@Override
	public List<URL> getJarFileUrls() {
		return jarFileUrls;
	}

	public void addJarFileUrl(URL jarFileUrl) {
		jarFileUrls.add( jarFileUrl );
	}

	public void addJarFileUrls(List<String> jarFiles) {
		jarFiles.forEach( jarFile -> addJarFileUrl( getURLFromPath( jarFile ) ) );
	}

	@Override
	public ClassLoader getClassLoader() {
		return null;
	}

	@Override
	public ClassLoader getTempClassLoader() {
		return null;
	}

	@Override
	public void pushClassTransformer(EnhancementContext enhancementContext) {
		// todo : log a message that this is currently not supported...
	}

	@Override
	public ClassTransformer getClassTransformer() {
		return null;
	}
}
