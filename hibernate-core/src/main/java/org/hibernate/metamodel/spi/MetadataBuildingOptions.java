/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.spi;

import java.util.List;

import javax.persistence.SharedCacheMode;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.CacheRegionDefinition;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.metamodel.archive.scan.spi.ScanEnvironment;
import org.hibernate.metamodel.archive.scan.spi.ScanOptions;
import org.hibernate.metamodel.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.metamodel.spi.relational.Database;
import org.hibernate.type.BasicType;
import org.jboss.jandex.IndexView;

/**
 * Describes the options used while building the Metadata object (during
 * {@link org.hibernate.metamodel.MetadataBuilder#build()} processing).
 *
 * @author Steve Ebersole
 */
public interface MetadataBuildingOptions {
	/**
	 * Access to the service registry.
	 *
	 * @return The service registry
	 */
	StandardServiceRegistry getServiceRegistry();

	/**
	 * Access to the database defaults.
	 *
	 * @return The database defaults
	 */
	Database.Defaults getDatabaseDefaults();

	/**
	 * Access the list of BasicType registrations.  These are the BasicTypes explicitly
	 * registered via calls to:<ul>
	 *     <li>{@link org.hibernate.metamodel.MetadataBuilder#with(org.hibernate.type.BasicType)}</li>
	 *     <li>{@link org.hibernate.metamodel.MetadataBuilder#with(org.hibernate.usertype.UserType, java.lang.String[])}</li>
	 *     <li>{@link org.hibernate.metamodel.MetadataBuilder#with(org.hibernate.usertype.CompositeUserType, java.lang.String[])}</li>
	 * </ul>
	 *
	 * @return The BasicType registrations
	 */
	List<BasicType> getBasicTypeRegistrations();

	/**
	 * Access to the Jandex index passed by call to
	 * {@link org.hibernate.metamodel.MetadataBuilder#with(org.jboss.jandex.IndexView)}, if any.
	 *
	 * @return The Jandex index
	 */
	IndexView getJandexView();

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
	 * Access to the ArchiveDescriptorFactory to be used for scanning
	 *
	 * @return The ArchiveDescriptorFactory
	 */
	ArchiveDescriptorFactory getArchiveDescriptorFactory();

	/**
	 * Access the temporary ClassLoader passed to us as defined by
	 * {@link javax.persistence.spi.PersistenceUnitInfo#getNewTempClassLoader()}, if any.
	 *
	 * @return The tempo ClassLoader
	 */
	ClassLoader getTempClassLoader();

	/**
	 * Access to the NamingStrategy which should be used.
	 *
	 * @return The NamingStrategy
	 */
	NamingStrategy getNamingStrategy();

	/**
	 * Access to the SharedCacheMode for determining whether we should perform second level
	 * caching or not.
	 *
	 * @return The SharedCacheMode
	 */
	SharedCacheMode getSharedCacheMode();

	/**
	 * Access to the default second level cache AccessType to use if not specified.
	 *
	 * @return The default AccessType
	 */
	AccessType getDefaultCacheAccessType();

	/**
	 * Access to whether we should be using the new identifier generator scheme.
	 * {@code true} indicates to use the new schema, {@code false} indicates to use the
	 * legacy scheme.
	 *
	 * @return Whether to use the new identifier generator scheme
	 */
	boolean isUseNewIdentifierGenerators();

	/**
	 * Access to the MultiTenancyStrategy for this environment.
	 *
	 * @return The MultiTenancyStrategy
	 */
	MultiTenancyStrategy getMultiTenancyStrategy();

	/**
	 * Access to all explicit cache region mappings.
	 *
	 * @return Explicit cache region mappings.
	 */
	List<CacheRegionDefinition> getCacheRegionDefinitions();

	/**
	 * Whether explicit discriminator declarations should be ignored for joined
	 * subclass style inheritance.
	 *
	 * @return {@code true} indicates they should be ignored; {@code false}
	 * indicates they should not be ignored.
	 *
	 * @see org.hibernate.metamodel.MetadataBuilder#withExplicitDiscriminatorsForJoinedSubclassSupport
	 * @see org.hibernate.cfg.AvailableSettings#IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
	 */
	boolean ignoreExplicitDiscriminatorsForJoinedInheritance();

	/**
	 * Whether we should do discrimination implicitly joined subclass style inheritance when no
	 * discriminator info is provided.
	 *
	 * @return {@code true} indicates we should do discrimination; {@code false} we should not.
	 *
	 * @see org.hibernate.metamodel.MetadataBuilder#withImplicitDiscriminatorsForJoinedSubclassSupport
	 * @see org.hibernate.cfg.AvailableSettings#IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
	 */
	boolean createImplicitDiscriminatorsForJoinedInheritance();

	/**
	 * Obtain the selected strategy for resolving members identifying persistent attributes
	 *
	 * @return The select resolver strategy
	 */
	PersistentAttributeMemberResolver getPersistentAttributeMemberResolver();
}
