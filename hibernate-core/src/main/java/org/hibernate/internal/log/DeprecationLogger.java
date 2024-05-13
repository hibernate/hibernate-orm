/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.internal.log;

import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.cfg.AvailableSettings;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * Class to consolidate logging about usage of deprecated features.
 *
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90000001, max = 90001000 )
@SubSystemLogging(
		name = DeprecationLogger.CATEGORY,
		description = "Logging related to uses of deprecated features"
)
public interface DeprecationLogger extends BasicLogger {
	String CATEGORY = SubSystemLogging.BASE + ".deprecation";

	DeprecationLogger DEPRECATION_LOGGER = Logger.getMessageLogger( DeprecationLogger.class, CATEGORY );

	@LogMessage(level = WARN)
	@Message(
			value = "embed-xml attributes were intended to be used for DOM4J entity mode. Since that entity mode has been " +
					"removed, embed-xml attributes are no longer supported and should be removed from mappings.",
			id = 90000004
	)
	void logDeprecationOfEmbedXmlSupport();

	@LogMessage(level = WARN)
	@Message(
			value = "Defining an entity [%s] with no physical id attribute is no longer supported; please map the " +
					"identifier to a physical entity attribute",
			id = 90000005
	)
	void logDeprecationOfNonNamedIdAttribute(String entityName);

//	/**
//	 * Log a warning about an attempt to specify no-longer-supported NamingStrategy
//	 *
//	 * @param setting - The old setting that indicates the NamingStrategy to use
//	 * @param implicitInstead - The new setting that indicates the ImplicitNamingStrategy to use
//	 * @param physicalInstead - The new setting that indicates the PhysicalNamingStrategy to use
//	 */
//	@LogMessage(level = WARN)
//	@Message(
//			value = "Attempted to specify unsupported NamingStrategy via setting [%s]; NamingStrategy " +
//					"has been removed in favor of the split ImplicitNamingStrategy and " +
//					"PhysicalNamingStrategy; use [%s] or [%s], respectively, instead.",
//			id = 90000006
//	)
//	void logDeprecatedNamingStrategySetting(String setting, String implicitInstead, String physicalInstead);

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

//	@LogMessage(level = WARN)
//	@Message(
//			value = "org.hibernate.hql.spi.TemporaryTableBulkIdStrategy (temporary) has been deprecated in favor of the" +
//					" more specific org.hibernate.hql.spi.id.local.LocalTemporaryTableBulkIdStrategy (local_temporary).",
//			id = 90000011
//	)
//	void logDeprecationOfTemporaryTableBulkIdStrategy();

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

//	@LogMessage(level = WARN)
//	@Message(
//			id = 90000016,
//			value = "Found use of deprecated 'collection property' syntax in HQL/JPQL query [%2$s.%1$s]; " +
//					"use collection function syntax instead [%1$s(%2$s)]."
//	)
//	void logDeprecationOfCollectionPropertiesInHql(String collectionPropertyName, String alias);

//	@LogMessage(level = WARN)
//	@Message(
//			id = 90000017,
//			value = "Found use of deprecated entity-type selector syntax in HQL/JPQL query ['%1$s.class']; use TYPE operator instead : type(%1$s)"
//	)
//	void logDeprecationOfClassEntityTypeSelector(String path);

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

//	@LogMessage(level = WARN)
//	@Message(
//			id = 90000020,
//			value = "You are using the deprecated legacy bytecode enhancement Ant-task.  This task is left in place for a short-time to " +
//					"aid migrations to 5.1 and the new (vastly improved) bytecode enhancement support.  This task (%s) now delegates to the" +
//					"new Ant-task (%s) leveraging that new bytecode enhancement.  You should update your build to use the new task explicitly."
//	)
//	void logDeprecatedInstrumentTask(Class taskClass, Class newTaskClass);

	@LogMessage(level = WARN)
	@Message(
			id = 90000021,
			value = "Encountered deprecated setting [%s], use [%s] instead"
	)
	void deprecatedSetting(String oldSettingName, String newSettingName);

//	@LogMessage(level = WARN)
//	@Message(
//			id = 90000022,
//			value = "Hibernate's legacy org.hibernate.Criteria API is deprecated; use the JPA jakarta.persistence.criteria.CriteriaQuery instead"
//	)
//	void deprecatedLegacyCriteria();

//	@LogMessage(level = WARN)
//	@Message(
//			id = 90000024,
//			value = "Application requested zero be used as the base for JDBC-style parameters found in native-queries; " +
//					"this is a *temporary* backwards-compatibility setting to help applications  using versions prior to " +
//					"5.3 in upgrading.  It will be removed in a later version."
//	)
//	void logUseOfDeprecatedZeroBasedJdbcStyleParams();

//	@LogMessage(level = WARN)
//	@Message(
//			id = 90000025,
//			value = "Encountered multiple component mappings for the same java class [%s] with different property mappings. " +
//					"This is deprecated and will be removed in a future version. Every property mapping combination should have its own java class"
//	)
//	void deprecatedComponentMapping(String name);

	@LogMessage(level = WARN)
	@Message(value = "%s does not need to be specified explicitly using 'hibernate.dialect' "
			+ "(remove the property setting and it will be selected by default)",
			id = 90000025)
	void automaticDialect(String dialect);

	@LogMessage(level = WARN)
	@Message(value = "%s has been deprecated",
			id = 90000026)
	void deprecatedDialect(String dialect);

	@LogMessage(level = WARN)
	@Message(value = "%s has been deprecated; use %s instead",
			id = 90000026)
	void deprecatedDialect(String dialect, String replacement);

	/**
	 * Different from {@link #deprecatedSetting} in that sometimes there is no
	 * direct alternative
	 */
	@LogMessage(level = WARN)
	@Message(
			id = 90000027,
			value = "Encountered deprecated setting [%s]; instead %s"
	)
	void deprecatedSetting2(String settingName, String alternative);

	/**
	 * Different from {@link #deprecatedSetting} in that sometimes there is no
	 * direct alternative
	 */
	@LogMessage(level = WARN)
	@Message(
			id = 90000028,
			value = "Support for `<hibernate-mappings/>` is deprecated [%s : %s]; " +
					"migrate to orm.xml or mapping.xml, or enable `" + AvailableSettings.TRANSFORM_HBM_XML +
					"` for on the fly transformation"
	)
	void logDeprecatedHbmXmlProcessing(SourceType sourceType, String name);

	/**
	 * Different from {@link #deprecatedSetting} in that sometimes there is no
	 * direct alternative
	 */
	@LogMessage(level = WARN)
	@Message(
			id = 90000029,
			value = "The [%s] configuration is deprecated and will be removed. Set the value to [%s] to get rid of this warning"
	)
	void deprecatedSettingForRemoval(String settingName, String defaultValue);

	/**
	 * Different from {@link #deprecatedSetting} in that sometimes there is no
	 * direct alternative
	 */
	@LogMessage(level = WARN)
	@Message(
			id = 90000030,
			value = "The [%s] configuration is deprecated and will be removed."
	)
	void deprecatedSettingNoReplacement(String settingName);

	@LogMessage(level = WARN)
	@Message(
			id = 90000031,
			value = "The native query colon escaping used for the [%s] operator is deprecated and will be removed. Use [%s] instead."
	)
	void deprecatedNativeQueryColonEscaping(String oldOperator, String newOperator);

	@LogMessage(level = WARN)
	@Message(
			id = 90000032,
			value = "The support for passing arrays to array_contains() is deprecated and will be removed. Use array_includes() instead."
	)
	void deprecatedArrayContainsWithArray();

}
