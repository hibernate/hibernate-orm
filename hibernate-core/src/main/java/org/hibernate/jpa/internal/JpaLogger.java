/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal;

import org.hibernate.Internal;
import org.hibernate.internal.SessionLogging;
import org.hibernate.internal.log.SubSystemLogging;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Miscellaneous logging related to Jakarta Persistence compatibility.
 */
@SubSystemLogging(
		name = SessionLogging.NAME,
		description = "Logging related to Jakarta Persistence compatibility"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min=8001,max = 10000)
@Internal
public interface JpaLogger extends BasicLogger {

	String NAME = SubSystemLogging.BASE + ".jpa";

	JpaLogger JPA_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), JpaLogger.class, NAME );

	@LogMessage(level = WARN)
	@Message(value = "Defining %s=true ignored in HEM", id = 8059)
	void definingFlushBeforeCompletionIgnoredInHem(String flushBeforeCompletion);

	@LogMessage(level = WARN)
	@Message(id = 8193, value = "Overriding %s is dangerous, this might break the JPA specification implementation")
	void overridingTransactionStrategyDangerous(String transactionStrategy);

	@LogMessage(level = INFO)
	@Message(id = 8318, value = "Could not find any META-INF/persistence.xml file in the classpath")
	void unableToFindPersistenceXmlInClasspath();

	@LogMessage(level = WARN)
	@Message(
			id = 8518,
			value = "Encountered multiple persistence-unit stanzas defining same name [%s]; persistence-unit names must be unique"
	)
	void duplicatedPersistenceUnitName(String name);

	@LogMessage(level = WARN)
	@Message(id = 8516, value = "Failed to discover types for enhancement from class: %s")
	void enhancementDiscoveryFailed(String className, @Cause Throwable cause);

	@LogMessage(level = DEBUG)
	@Message(id = 8520, value = "Removed integration override setting [%s] due to normalization")
	void removedIntegrationOverride(String key);

	@LogMessage(level = DEBUG)
	@Message(id = 8521, value = "Removed merged setting [%s] due to normalization")
	void removedMergedSetting(String key);

	@LogMessage(level = TRACE)
	@Message(id = 8522, value = "Starting createEntityManagerFactory for persistenceUnitName %s")
	void startingCreateEntityManagerFactory(String persistenceUnitName);

	@LogMessage(level = TRACE)
	@Message(id = 8523, value = "Could not obtain matching EntityManagerFactoryBuilder, returning %s")
	void couldNotObtainEmfBuilder(String result);

	@LogMessage(level = TRACE)
	@Message(id = 8524, value = "Attempting to obtain correct EntityManagerFactoryBuilder for persistenceUnitName : %s")
	void attemptingToObtainEmfBuilder(String persistenceUnitName);

	@LogMessage(level = TRACE)
	@Message(id = 8525, value = "Located and parsed %s persistence units; checking each")
	void locatedAndParsedPersistenceUnits(int count);

	@LogMessage(level = TRACE)
	@Message(id = 8526, value = "Checking persistence-unit [name=%s, explicit-provider=%s] against incoming persistence unit name [%s]")
	void checkingPersistenceUnitName(String name, String explicitProvider, String incomingName);

	@LogMessage(level = TRACE)
	@Message(id = 8527, value = "Excluding from consideration due to name mismatch")
	void excludingDueToNameMismatch();

	@LogMessage(level = TRACE)
	@Message(id = 8528, value = "Excluding from consideration due to provider mismatch")
	void excludingDueToProviderMismatch();

	@LogMessage(level = DEBUG)
	@Message(id = 8529, value = "Found no matching persistence units")
	void foundNoMatchingPersistenceUnits();

	@LogMessage(level = DEBUG)
	@Message(id = 8530, value = "Unable to locate persistence units")
	void unableToLocatePersistenceUnits(@Cause Throwable t);

	@LogMessage(level = TRACE)
	@Message(id = 8531, value = "Starting createContainerEntityManagerFactory : %s")
	void startingCreateContainerEntityManagerFactory(String name);

	@LogMessage(level = TRACE)
	@Message(id = 8532, value = "Starting generateSchema : PUI.name=%s")
	void startingGenerateSchemaForPuiName(String name);

	@LogMessage(level = TRACE)
	@Message(id = 8533, value = "Starting generateSchema for persistenceUnitName %s")
	void startingGenerateSchema(String name);

	@LogMessage(level = TRACE)
	@Message(id = 8535, value = "Attempting to parse persistence.xml file : %s")
	void attemptingToParsePersistenceXml(String url);

	@LogMessage(level = TRACE)
	@Message(id = 8536, value = "Persistence unit name from persistence.xml: '%s'")
	void persistenceUnitNameFromXml(String name);

	@LogMessage(level = TRACE)
	@Message(id = 8537, value = "Pushing class transformers for PU named '%s' on loading classloader %s")
	void pushingClassTransformers(String persistenceUnitName, String loadingClassLoader);

	@LogMessage(level = DEBUG)
	@Message(id = 8538, value = "PersistenceUnitTransactionType defaulting to RESOURCE_LOCAL")
	void fallingBackToResourceLocal();

	@LogMessage(level = INFO)
	@Message(id = 8540, value = "Processing PersistenceUnitInfo [name: %s]")
	void processingPersistenceUnitInfo(String persistenceUnitName);

	@LogMessage(level = DEBUG)
	@Message(id = 8541, value = "%s")
	void processingPersistenceUnitInfoDetails(String string);

	// ProviderChecker

	@LogMessage(level = TRACE)
	@Message(id = 8542, value = "Checking requested PersistenceProvider name [%s] against Hibernate provider names")
	void checkingRequestedPersistenceProviderName(String requestedProviderName);

	@LogMessage(level = DEBUG)
	@Message(id = 8543, value = "Integration provided explicit PersistenceProvider [%s]")
	void integrationProvidedExplicitPersistenceProvider(String providerName);

	@LogMessage(level = DEBUG)
	@Message(id = 8544, value = "Persistence-unit [%s] requested PersistenceProvider [%s]")
	void persistenceUnitRequestedPersistenceProvider(String persistenceUnitName, String providerName);

	@LogMessage(level = DEBUG)
	@Message(id = 8545, value = "No PersistenceProvider explicitly requested, assuming Hibernate")
	void noPersistenceProviderExplicitlySpecified();

	// FlushModeTypeHelper

	@LogMessage(level = DEBUG)
	@Message(id = 8546,
			value = """
					Interpreting Hibernate 'FlushMode.ALWAYS' as JPA 'FlushModeType.AUTO'\
					(may cause problems if relying on FlushMode.ALWAYS-specific behavior)""")
	void interpretingFlushModeAlwaysAsJpaAuto();

	@LogMessage(level = DEBUG)
	@Message(id = 8547,
			value = """
					Interpreting Hibernate 'FlushMode.MANUAL' as JPA 'FlushModeType.COMMIT'\
					(may cause problems if relying on FlushMode.MANUAL-specific behavior)""")
	void interpretingFlushModeManualAsJpaCommit();

	@LogMessage(level = TRACE)
	@Message(id = 8548, value = "Attempting to interpret external setting [%s] as FlushMode name")
	void attemptingToInterpretExternalSettingAsFlushModeName(String externalName);

	@LogMessage(level = TRACE)
	@Message(id = 8549, value = "Attempting to interpret external setting [%s] as FlushModeType name")
	void attemptingToInterpretExternalSettingAsFlushModeTypeName(String externalName);
}
