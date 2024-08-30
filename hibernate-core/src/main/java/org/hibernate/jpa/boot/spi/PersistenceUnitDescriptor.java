/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.boot.spi;

import java.net.URL;
import java.util.List;
import java.util.Properties;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.spi.ClassTransformer;

/**
 * Abstraction for dealing with either {@code <persistence-unit/>} information whether that comes from
 * an EE container in the form of {@link jakarta.persistence.spi.PersistenceUnitInfo} or in an SE environment
 * where Hibernate has parsed the {@code persistence.xml} file itself.
 *
 * @author Steve Ebersole
 */
public interface PersistenceUnitDescriptor {
	/**
	 * Get the root url for the persistence unit.  Intended to describe the base for scanning.
	 *
	 * @return The root url
	 */
	URL getPersistenceUnitRootUrl();

	/**
	 * Get the persistence unit name,
	 *
	 * @return The persistence unit name,
	 */
	String getName();

	/**
	 * Get the explicitly specified provider class name, or {@code null} if not specified.
	 *
	 * @return The specified provider class name
	 */
	String getProviderClassName();

	/**
	 * Is the use of quoted identifiers in effect for this whole persistence unit?
	 *
	 * @return {@code true} is quoted identifiers should be used throughout the unit.
	 */
	boolean isUseQuotedIdentifiers();

	/**
	 * Essentially should scanning for classes be performed?  If not, the list of classes available is limited to:<ul>
	 *     <li>classes listed in {@link #getManagedClassNames()}</li>
	 *     <li>classes named in all {@link #getMappingFileNames}</li>
	 *     <li>classes discovered in {@link #getJarFileUrls}</li>
	 * </ul>
	 *
	 * @return {@code true} if the root url should not be scanned for classes.
	 */
	boolean isExcludeUnlistedClasses();

	PersistenceUnitTransactionType getTransactionType();

	ValidationMode getValidationMode();

	SharedCacheMode getSharedCacheMode();

	List<String> getManagedClassNames();

	List<String> getMappingFileNames();

	List<URL> getJarFileUrls();

	Object getNonJtaDataSource();

	Object getJtaDataSource();

	Properties getProperties();

	ClassLoader getClassLoader();

	ClassLoader getTempClassLoader();

	void pushClassTransformer(EnhancementContext enhancementContext);

	ClassTransformer getClassTransformer();
}
