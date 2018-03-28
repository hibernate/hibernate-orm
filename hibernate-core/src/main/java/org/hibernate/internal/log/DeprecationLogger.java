/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.log;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Class to consolidate logging about usage of deprecated features.
 *
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90000001, max = 90001000 )
public interface DeprecationLogger extends BasicLogger {
	String CATEGORY = "org.hibernate.orm.deprecation";

	DeprecationLogger DEPRECATION_LOGGER = Logger.getMessageLogger(
			DeprecationLogger.class,
			CATEGORY
	);

	/**
	 * Log about usage of deprecated Scanner setting
	 */
	@LogMessage( level = INFO )
	@Message(
			value = "Found usage of deprecated setting for specifying Scanner [hibernate.ejb.resource_scanner]; " +
					"use [hibernate.archive.scanner] instead",
			id = 90000001
	)
	public void logDeprecatedScannerSetting();

	/**
	 * Log message indicating the use of multiple EntityModes for a single entity.
	 */
	@LogMessage( level = WARN )
	@Message(
			value = "Support for an entity defining multiple entity-modes is deprecated",
			id = 90000002
	)
	public void logDeprecationOfMultipleEntityModeSupport();

	/**
	 * Log message indicating the use of DOM4J EntityMode.
	 */
	@LogMessage( level = WARN )
	@Message(
			value = "Use of DOM4J entity-mode is considered deprecated",
			id = 90000003
	)
	public void logDeprecationOfDomEntityModeSupport();

	@LogMessage(level = WARN)
	@Message(
			value = "embed-xml attributes were intended to be used for DOM4J entity mode. Since that entity mode has been " +
					"removed, embed-xml attributes are no longer supported and should be removed from mappings.",
			id = 90000004
	)
	public void logDeprecationOfEmbedXmlSupport();

	@LogMessage(level = WARN)
	@Message(
			value = "Defining an entity [%s] with no physical id attribute is no longer supported; please map the " +
					"identifier to a physical entity attribute",
			id = 90000005
	)
	public void logDeprecationOfNonNamedIdAttribute(String entityName);

	/**
	 * Log a warning about an attempt to specify no-longer-supported NamingStrategy
	 *
	 * @param setting - The old setting that indicates the NamingStrategy to use
	 * @param implicitInstead - The new setting that indicates the ImplicitNamingStrategy to use
	 * @param physicalInstead - The new setting that indicates the PhysicalNamingStrategy to use
	 */
	@LogMessage(level = WARN)
	@Message(
			value = "Attempted to specify unsupported NamingStrategy via setting [%s]; NamingStrategy " +
					"has been removed in favor of the split ImplicitNamingStrategy and " +
					"PhysicalNamingStrategy; use [%s] or [%s], respectively, instead.",
			id = 90000006
	)
	void logDeprecatedNamingStrategySetting(String setting, String implicitInstead, String physicalInstead);

	/**
	 * Log a warning about an attempt to specify unsupported NamingStrategy
	 */
	@LogMessage(level = WARN)
	@Message(
			value = "Attempted to specify unsupported NamingStrategy via command-line argument [--naming]. " +
					"NamingStrategy has been removed in favor of the split ImplicitNamingStrategy and " +
					"PhysicalNamingStrategy; use [--implicit-naming] or [--physical-naming], respectively, instead.",
			id = 90000007
	)
	void logDeprecatedNamingStrategyArgument();

	/**
	 * Log a warning about an attempt to specify unsupported NamingStrategy
	 */
	@LogMessage(level = WARN)
	@Message(
			value = "Attempted to specify unsupported NamingStrategy via Ant task argument. " +
					"NamingStrategy has been removed in favor of the split ImplicitNamingStrategy and " +
					"PhysicalNamingStrategy.",
			id = 90000008
	)
	void logDeprecatedNamingStrategyAntArgument();

	@LogMessage(level = WARN)
	@Message(
			value = "The outer-join attribute on <many-to-many> has been deprecated. " +
					"Instead of outer-join=\"false\", use lazy=\"extra\" with <map>, <set>, " +
					"<bag>, <idbag>, or <list>, which will only initialize entities (not as " +
					"a proxy) as needed.",
			id = 90000009
	)
	void deprecatedManyToManyOuterJoin();

	@LogMessage(level = WARN)
	@Message(
			value = "The fetch attribute on <many-to-many> has been deprecated. " +
					"Instead of fetch=\"select\", use lazy=\"extra\" with <map>, <set>, " +
					"<bag>, <idbag>, or <list>, which will only initialize entities (not as " +
					"a proxy) as needed.",
			id = 90000010
	)
	void deprecatedManyToManyFetch();


	@LogMessage(level = WARN)
	@Message(
			value = "org.hibernate.hql.spi.TemporaryTableBulkIdStrategy (temporary) has been deprecated in favor of the" +
					" more specific org.hibernate.hql.spi.id.local.LocalTemporaryTableBulkIdStrategy (local_temporary).",
			id = 90000011
	)
	void logDeprecationOfTemporaryTableBulkIdStrategy();

	@LogMessage(level = WARN)
	@Message(value = "Recognized obsolete hibernate namespace %s. Use namespace %s instead.  Support for obsolete DTD/XSD namespaces may be removed at any time.",
			id = 90000012)
	void recognizedObsoleteHibernateNamespace(
			String oldHibernateNamespace,
			String hibernateNamespace);

	@LogMessage(level = WARN)
	@Message(
			id = 90000013,
			value = "Named ConnectionProvider [%s] has been deprecated in favor of %s; that provider will be used instead.  Update your settings"
	)
	void connectionProviderClassDeprecated(
			String providerClassName,
			String actualProviderClassName);

	@LogMessage(level = WARN)
	@Message(
			id = 90000014,
			value = "Found use of deprecated [%s] sequence-based id generator; " +
					"use org.hibernate.id.enhanced.SequenceStyleGenerator instead.  " +
					"See Hibernate Domain Model Mapping Guide for details."
	)
	void deprecatedSequenceGenerator(String generatorImpl);

	@LogMessage(level = WARN)
	@Message(
			id = 90000015,
			value = "Found use of deprecated [%s] table-based id generator; " +
					"use org.hibernate.id.enhanced.TableGenerator instead.  " +
					"See Hibernate Domain Model Mapping Guide for details."
	)
	void deprecatedTableGenerator(String generatorImpl);

	@LogMessage(level = WARN)
	@Message(
			id = 90000016,
			value = "Found use of deprecated 'collection property' syntax in HQL/JPQL query [%2$s.%1$s]; " +
					"use collection function syntax instead [%1$s(%2$s)]."
	)
	void logDeprecationOfCollectionPropertiesInHql(String collectionPropertyName, String alias);

	@LogMessage(level = WARN)
	@Message(
			id = 90000017,
			value = "Found use of deprecated entity-type selector syntax in HQL/JPQL query ['%1$s.class']; use TYPE operator instead : type(%1$s)"
	)
	void logDeprecationOfClassEntityTypeSelector(String path);

	@LogMessage(level = WARN)
	@Message(
			id = 90000018,
			value = "Found use of deprecated transaction factory setting [%s]; use the new TransactionCoordinatorBuilder settings [%s] instead"
	)
	void logDeprecatedTransactionFactorySetting(String legacySettingName, String updatedSettingName);

//	@LogMessage(level = WARN)
//	@Message(
//			id = 90000019,
//			value = "You are using the deprecated legacy bytecode enhancement feature which has been superseded by a vastly improved bytecode enhancer."
//	)
//	void logDeprecatedBytecodeEnhancement();

	@LogMessage(level = WARN)
	@Message(
			id = 90000020,
			value = "You are using the deprecated legacy bytecode enhancement Ant-task.  This task is left in place for a short-time to " +
					"aid migrations to 5.1 and the new (vastly improved) bytecode enhancement support.  This task (%s) now delegates to the" +
					"new Ant-task (%s) leveraging that new bytecode enhancement.  You should update your build to use the new task explicitly."
	)
	void logDeprecatedInstrumentTask(Class taskClass, Class newTaskClass);

	@LogMessage(level = WARN)
	@Message(
			id = 90000021,
			value = "Encountered deprecated setting [%s], use [%s] instead"
	)
	void deprecatedSetting(String oldSettingName, String newSettingName);

	@LogMessage(level = WARN)
	@Message(
			id = 90000022,
			value = "Hibernate's legacy org.hibernate.Criteria API is deprecated; use the JPA javax.persistence.criteria.CriteriaQuery instead"
	)
	void deprecatedLegacyCriteria();

	@LogMessage(level = WARN)
	@Message(
			id = 90000023,
			value = "Encountered use of deprecated Connection handling settings [hibernate.connection.acquisition_mode]" +
					"or [hibernate.connection.release_mode]; use [hibernate.connection.handling_mode] instead"
	)
	void logUseOfDeprecatedConnectionHandlingSettings();

	@LogMessage(level = WARN)
	@Message(
			id = 90000024,
			value = "Application requested zero be used as the base for JDBC-style parameters found in native-queries; " +
					"this is a *temporary* backwards-compatibility setting to help applications  using versions prior to " +
					"5.3 in upgrading.  It will be removed in a later version."
	)
	void logUseOfDeprecatedZeroBasedJdbcStyleParams();
}
