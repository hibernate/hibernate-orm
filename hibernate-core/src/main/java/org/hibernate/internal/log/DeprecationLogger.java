/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.log;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.Internal;
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
@Internal
public interface DeprecationLogger extends BasicLogger {
	String CATEGORY = SubSystemLogging.BASE + ".deprecation";

	DeprecationLogger DEPRECATION_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), DeprecationLogger.class, CATEGORY );

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
	@Message(value = "Recognized obsolete hibernate namespace %s. Use namespace %s instead.  Support for obsolete DTD/XSD namespaces may be removed at any time.",
			id = 90000012)
	void recognizedObsoleteHibernateNamespace(
			String oldHibernateNamespace,
			String hibernateNamespace);

	@LogMessage(level = WARN)
	@Message(
			id = 90000018,
			value = "Found use of deprecated transaction factory setting [%s]; use the new TransactionCoordinatorBuilder settings [%s] instead"
	)
	void logDeprecatedTransactionFactorySetting(String legacySettingName, String updatedSettingName);

	@LogMessage(level = WARN)
	@Message(
			id = 90000021,
			value = "Encountered deprecated setting [%s], use [%s] instead"
	)
	void deprecatedSetting(String oldSettingName, String newSettingName);

	@LogMessage(level = WARN)
	@Message(
			id = 90000022,
			value = "Encountered deprecated setting [%s]"
	)
	void deprecatedSetting(String settingName);

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

	@LogMessage(level = WARN)
	@Message(
			id = 90000033,
			value = "Encountered use of deprecated annotation [%s] at %s."
	)
	void deprecatedAnnotation(Class<? extends Annotation> annotationType, String locationDescription);

	@LogMessage(level = WARN)
	@Message(
			id = 90000034,
			value = "Refreshing/locking detached entities is no longer allowed."
	)
	void deprecatedRefreshLockDetachedEntity();

	@LogMessage(level = WARN)
	@Message(
			id = 90000035,
			value = "Callback method annotated '@%s' declared by embeddable class '%s'"
					+ " relies on an undocumented and unsupported capability"
					+ " (lifecycle callback methods should be declared by entity classes)"
	)
	void embeddableLifecycleCallback(String annotationType, String embeddable);

	@LogMessage(level = WARN)
	@Message(
			id = 90000036,
			value = "Encountered deprecated hint [%s]"
	)
	void deprecatedHint(String deprecatedHint);

	@LogMessage(level = WARN)
	@Message(
			id = 90000037,
			value = "Encountered deprecated hint [%s], use [%s] instead"
	)
	void deprecatedHint(String deprecatedHint, String replacementHint);

	@LogMessage(level = WARN)
	@Message(
			id = 90000038,
			value = "Encountered deprecated value for JtaPlatform setting [%s]: [%s]; use a non-deprecated value among %s instead"
	)
	void deprecatedJtaPlatformSetting(String settingName, String deprecatedValue, List<String> replacements);

	@LogMessage(level = WARN)
	@Message(value = "Using %s which does not generate IETF RFC 4122 compliant UUID values; consider using %s instead", id = 90303)
	void deprecatedUuidHexGenerator(String name, String name2);

	@LogMessage(level = WARN)
	@Message(value = "DEPRECATED: use [%s] instead with custom [%s] implementation", id = 90304)
	void deprecatedUuidGenerator(String name, String name2);
}
