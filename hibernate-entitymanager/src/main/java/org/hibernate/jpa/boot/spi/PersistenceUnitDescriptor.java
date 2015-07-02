/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.boot.spi;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;

/**
 * Abstraction for dealing with either {@code <persistence-unit/>} information whether that comes from
 * an EE container in the form of {@link javax.persistence.spi.PersistenceUnitInfo} or in an SE environment
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
	public URL getPersistenceUnitRootUrl();

	/**
	 * Get the persistence unit name,
	 *
	 * @return The persistence unit name,
	 */
	public String getName();

	/**
	 * Get the explicitly specified provider class name, or {@code null} if not specified.
	 *
	 * @return The specified provider class name
	 */
	public String getProviderClassName();

	/**
	 * Is the use of quoted identifiers in effect for this whole persistence unit?
	 *
	 * @return {@code true} is quoted identifiers should be used throughout the unit.
	 */
	public boolean isUseQuotedIdentifiers();

	/**
	 * Essentially should scanning for classes be performed?  If not, the list of classes available is limited to:<ul>
	 *     <li>classes listed in {@link #getManagedClassNames()}</li>
	 *     <li>classes named in all {@link #getMappingFileNames}</li>
	 *     <li>classes discovered in {@link #getJarFileUrls}</li>
	 * </ul>
	 *
	 * @return {@code true} if the root url should not be scanned for classes.
	 */
	public boolean isExcludeUnlistedClasses();

	public PersistenceUnitTransactionType getTransactionType();

	public ValidationMode getValidationMode();

	public SharedCacheMode getSharedCacheMode();

	public List<String> getManagedClassNames();

	public List<String> getMappingFileNames();

	public List<URL> getJarFileUrls();

	public Object getNonJtaDataSource();

	public Object getJtaDataSource();

	public Properties getProperties();

	public ClassLoader getClassLoader();
	public ClassLoader getTempClassLoader();

	public void pushClassTransformer(Collection<String> entityClassNames);
}
