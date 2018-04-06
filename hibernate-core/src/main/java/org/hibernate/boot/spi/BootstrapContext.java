/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import java.util.Collection;
import java.util.Map;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.AttributeConverterInfo;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.jpa.spi.MutableJpaCompliance;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.jandex.IndexView;

/**
 * Defines a context for things generally available to the process of
 * bootstrapping a SessionFactory that are expected to be released after
 * the SessionFactory is built.
 *
 * @author Steve Ebersole
 */
public interface BootstrapContext {
	StandardServiceRegistry getServiceRegistry();

	MutableJpaCompliance getJpaCompliance();

	TypeConfiguration getTypeConfiguration();

	MetadataBuildingOptions getMetadataBuildingOptions();

	boolean isJpaBootstrap();

	/**
	 * Indicates that bootstrap was initiated from JPA bootstrapping.  Internally {@code false} is
	 * the assumed value.  We only need to call this to mark that as true.
	 */
	void markAsJpaBootstrap();

	/**
	 * Access the temporary ClassLoader passed to us as defined by
	 * {@link javax.persistence.spi.PersistenceUnitInfo#getNewTempClassLoader()}, if any.
	 *
	 * @return The tempo ClassLoader
	 */
	ClassLoader getJpaTempClassLoader();

	ClassLoaderAccess getClassLoaderAccess();

	/**
	 * Access to the shared Classmate objects used throughout Hibernate's
	 * bootstrap process.
	 *
	 * @return Access to the shared Classmate delegates.
	 */
	ClassmateContext getClassmateContext();

	/**
	 * Access to the ArchiveDescriptorFactory to be used for scanning
	 *
	 * @return The ArchiveDescriptorFactory
	 */
	ArchiveDescriptorFactory getArchiveDescriptorFactory();

	/**
	 * Access to the options to be used for scanning
	 *
	 * @return The scan options
	 */
	ScanOptions getScanOptions();

	/**
	 * Access to the environment for scanning.  Consider this temporary; see discussion on
	 * {@link ScanEnvironment}
	 *
	 * @return The scan environment
	 */
	ScanEnvironment getScanEnvironment();

	/**
	 * Access to the Scanner to be used for scanning.  Can be:<ul>
	 *     <li>A Scanner instance</li>
	 *     <li>A Class reference to the Scanner implementor</li>
	 *     <li>A String naming the Scanner implementor</li>
	 * </ul>
	 *
	 * @return The scanner
	 */
	Object getScanner();

	/**
	 * Retrieve the Hibernate Commons Annotations ReflectionManager to use.
	 *
	 * @return The Hibernate Commons Annotations ReflectionManager to use.
	 *
	 * @deprecated Deprecated (with no replacement) to indicate that this will go away as
	 * we migrate away from Hibernate Commons Annotations to Jandex for annotation handling
	 * and XMl->annotation merging.
	 */
	@Deprecated
	ReflectionManager getReflectionManager();

	/**
	 * Access to the Jandex index passed by call to
	 * {@link org.hibernate.boot.MetadataBuilder#applyIndexView(org.jboss.jandex.IndexView)}, if any.
	 * <p/>
	 * Note that Jandex is currently not used.  See https://github.com/hibernate/hibernate-orm/wiki/Roadmap7.0
	 *
	 * @return The Jandex index
	 */
	IndexView getJandexView();

	/**
	 * Access to any SQL functions explicitly registered with the MetadataBuilder.  This
	 * does not include Dialect defined functions, etc.
	 * <p/>
	 * Should never return {@code null}
	 *
	 * @return The SQLFunctions registered through MetadataBuilder
	 */
	Map<String,SQLFunction> getSqlFunctions();

	/**
	 * Access to any AuxiliaryDatabaseObject explicitly registered with the MetadataBuilder.  This
	 * does not include AuxiliaryDatabaseObject defined in mappings.
	 * <p/>
	 * Should never return {@code null}
	 *
	 * @return The AuxiliaryDatabaseObject registered through MetadataBuilder
	 */
	Collection<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjectList();

	/**
	 * Access to collected AttributeConverter definitions.
	 * <p/>
	 * Should never return {@code null}
	 *
	 * @return The AttributeConverterInfo registered through MetadataBuilder
	 */
	Collection<AttributeConverterInfo> getAttributeConverters();

	/**
	 * Access to all explicit cache region mappings.
	 * <p/>
	 * Should never return {@code null}
	 *
	 * @return Explicit cache region mappings
	 */
	Collection<CacheRegionDefinition> getCacheRegionDefinitions();

	/**
	 * Releases the "bootstrap only" resources held by this BootstrapContext.
	 * <p/>
	 * Only one call to this method is supported, after we have completed the process of
	 * building the (non-inflight) Metadata impl.  We may want to delay this until we
	 * get into SF building.  Not sure yet.
	 *
	 * @todo verify this ^^
	 */
	void release();
}
