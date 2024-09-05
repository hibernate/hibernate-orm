/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.cfg;

import org.hibernate.Incubating;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;

import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * Settings related to persistence-units
 *
 * @author Steve Ebersole
 */
public interface PersistenceSettings {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Specifies a class implementing {@link jakarta.persistence.spi.PersistenceProvider}.
	 * Naturally, this should always be {@link org.hibernate.jpa.HibernatePersistenceProvider},
	 * which is the best damn persistence provider ever. There's no need to explicitly specify
	 * this setting when there are no inferior persistence providers floating about.
	 * <p>
	 * See JPA 2 sections 9.4.3 and 8.2.1.4
	 */
	String JAKARTA_PERSISTENCE_PROVIDER = "jakarta.persistence.provider";

	/**
	 * Specifies the {@linkplain jakarta.persistence.PersistenceUnitTransactionType
	 * type of transactions} supported by the entity managers. The default depends on
	 * whether the program is considered to be executing in a Java SE or EE environment:
	 * <ul>
	 *     <li>For Java SE, the default is
	 *     {@link jakarta.persistence.PersistenceUnitTransactionType#RESOURCE_LOCAL
	 *     RESOURCE_LOCAL}.
	 *     <li>For Java EE, the default is
	 *     {@link jakarta.persistence.PersistenceUnitTransactionType#JTA JTA}.
	 * </ul>
	 * <p>
	 * See JPA 2 sections 9.4.3 and 8.2.1.2
	 *
	 * @see PersistenceUnitInfo#getTransactionType()
	 */
	String JAKARTA_TRANSACTION_TYPE = "jakarta.persistence.transactionType";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Hibernate settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Setting used to name the Hibernate {@link org.hibernate.SessionFactory}.
	 * <p>
	 * Naming the SessionFactory allows for it to be properly serialized across JVMs as
	 * long as the same name is used on each JVM.
	 * <p>
	 * If {@link #SESSION_FACTORY_NAME_IS_JNDI} is set to {@code true}, this name will
	 * also be used as {@link #SESSION_FACTORY_JNDI_NAME}.
	 *
	 * @see #SESSION_FACTORY_JNDI_NAME
	 * @see org.hibernate.internal.SessionFactoryRegistry
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyName(String)
	 */
	String SESSION_FACTORY_NAME = "hibernate.session_factory_name";

	/**
	 * An optional name used to bind the SessionFactory into JNDI.
	 * <p>
	 * If {@link #SESSION_FACTORY_NAME_IS_JNDI} is set to {@code true},
	 * {@link #SESSION_FACTORY_NAME} will be used as the JNDI name
	 *
	 * @see #SESSION_FACTORY_NAME_IS_JNDI
	 * @see org.hibernate.internal.SessionFactoryRegistry
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyName(String)
	 */
	String SESSION_FACTORY_JNDI_NAME = "hibernate.session_factory_jndi_name";

	/**
	 * Does the value defined by {@link #SESSION_FACTORY_NAME} represent a JNDI namespace
	 * into which the {@link org.hibernate.SessionFactory} should be bound and made accessible?
	 * <p>
	 * Defaults to {@code true} for backwards compatibility.
	 * <p>
	 * Set this to {@code false} if naming a SessionFactory is needed for serialization purposes,
	 * but no writable JNDI context exists in the runtime environment or if the user simply does
	 * not want JNDI to be used.
	 *
	 * @see #SESSION_FACTORY_NAME
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyNameAsJndiName(boolean)
	 *
	 * @settingDefault {@code true} if {@link SessionFactory#getName()} comes from
	 * {@value #SESSION_FACTORY_NAME}; {@code false} if there is no {@link SessionFactory#getName()}
	 * or if it comes from {@value #PERSISTENCE_UNIT_NAME}
	 */
	String SESSION_FACTORY_NAME_IS_JNDI = "hibernate.session_factory_name_is_jndi";

	/**
	 * Specifies the name of the persistence unit.
	 *
	 * @see PersistenceUnitInfo#getPersistenceUnitName()
	 */
	String PERSISTENCE_UNIT_NAME = "hibernate.persistenceUnitName";

	/**
	 * Specifies an implementation of {@link org.hibernate.boot.archive.scan.spi.Scanner},
	 * either:
	 * <ul>
	 *     <li>an instance of {@code Scanner},
	 *     <li>a {@link Class} representing a class that implements {@code Scanner}
	 *     <li>the name of a class that implements {@code Scanner}.
	 * </ul>
	 *
	 * @see org.hibernate.boot.MetadataBuilder#applyScanner
	 */
	String SCANNER = "hibernate.archive.scanner";

	/**
	 * Specifies an {@link org.hibernate.boot.archive.spi.ArchiveDescriptorFactory} to use
	 * in the scanning process, either:
	 * <ul>
	 *     <li>an instance of {@code ArchiveDescriptorFactory},
	 *     <li>a {@link Class} representing a class that implements {@code ArchiveDescriptorFactory}, or
	 *     <li>the name of a class that implements {@code ArchiveDescriptorFactory}.
	 * </ul>
	 * <p>
	 * See information on {@link org.hibernate.boot.archive.scan.spi.Scanner}
	 * about expected constructor forms.
	 *
	 * @see #SCANNER
	 * @see org.hibernate.boot.archive.scan.spi.Scanner
	 * @see org.hibernate.boot.archive.scan.spi.AbstractScannerImpl
	 * @see org.hibernate.boot.MetadataBuilder#applyArchiveDescriptorFactory
	 */
	String SCANNER_ARCHIVE_INTERPRETER = "hibernate.archive.interpreter";

	/**
	 * Identifies a comma-separated list of values indicating the types of things we should
	 * auto-detect during scanning. Allowable values include:
	 * <ul>
	 *     <li>{@code "class"} specifies that {@code .class} files are discovered as managed classes
	 *     <li>{@code "hbm"} specifies that {@code hbm.xml} files are discovered as mapping files
	 * </ul>
	 *
	 * @see org.hibernate.boot.MetadataBuilder#applyScanOptions
	 */
	String SCANNER_DISCOVERY = "hibernate.archive.autodetection";

	/**
	 * Allows JPA callbacks (via {@link jakarta.persistence.PreUpdate} and friends) to be
	 * completely disabled. Mostly useful to save some memory when they are not used.
	 * <p>
	 * JPA callbacks are enabled by default. Set this property to {@code false} to disable
	 * them.
	 * <p>
	 * Experimental and will likely be removed as soon as the memory overhead is resolved.
	 *
	 * @see org.hibernate.jpa.event.spi.CallbackType
	 *
	 * @since 5.4
	 */
	@Incubating
	String JPA_CALLBACKS_ENABLED = "hibernate.jpa_callbacks.enabled";

	/**
	 * Specifies a class which implements {@link org.hibernate.SessionFactoryObserver} and has
	 * a constructor with no parameters.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#addSessionFactoryObservers(SessionFactoryObserver...)
	 */
	String SESSION_FACTORY_OBSERVER = "hibernate.session_factory_observer";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Legacy JPA settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * @deprecated Use {@link #JAKARTA_PERSISTENCE_PROVIDER} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_PERSISTENCE_PROVIDER = "javax.persistence.provider";

	/**
	 * The type of transactions supported by the entity managers.
	 * <p>
	 * See JPA 2 sections 9.4.3 and 8.2.1.2
	 *
	 * @deprecated Use {@link #JAKARTA_TRANSACTION_TYPE} instead
	 */
	@Deprecated
	String JPA_TRANSACTION_TYPE = "javax.persistence.transactionType";

	/**
	 * Specifies whether unowned (i.e. {@code mapped-by}) associations should be considered
	 * when validating transient entity instance references.
	 *
	 * @settingDefault {@code false}
	 */
	String UNOWNED_ASSOCIATION_TRANSIENT_CHECK = "hibernate.unowned_association_transient_check";
}
