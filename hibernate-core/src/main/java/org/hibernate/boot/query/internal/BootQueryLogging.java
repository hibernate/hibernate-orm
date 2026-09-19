/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query.internal;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import org.hibernate.Internal;
import org.hibernate.boot.BootLogging;
import org.hibernate.boot.query.HbmResultSetMappingDescriptor;
import org.hibernate.internal.log.SubSystemLogging;
import org.hibernate.spi.NavigablePath;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.TRACE;

/**
 * @author Steve Ebersole
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 160301, max = 160400)
@SubSystemLogging(
		name = BootQueryLogging.NAME,
		description = "Logging related to processing of named queries"
)
@Internal
public interface BootQueryLogging extends BasicLogger {
	String NAME = BootLogging.NAME + ".query";
	BootQueryLogging BOOT_QUERY_LOGGER = Logger.getMessageLogger(
			MethodHandles.lookup(), BootQueryLogging.class, NAME, Locale.ROOT
	);

	@LogMessage(level = TRACE)
	@Message(id = 160301, value = "Creating explicit HbmResultSetMappingDescriptor: %s")
	void creatingExplicitHbmResultSetMapping(String registrationName);

	@LogMessage(level = TRACE)
	@Message(id = 160302, value = "Resolving HbmResultSetMappingDescriptor into memento for [%s]")
	void resolvingHbmResultSetMapping(String registrationName);

	@LogMessage(level = TRACE)
	@Message(id = 160303, value = "Creating EntityResultDescriptor (%s : %s) for ResultSet mapping - %s")
	void creatingEntityResult(String tableAlias, String entityName, String registrationName);

	@LogMessage(level = TRACE)
	@Message(id = 160304, value = "Resolving HBM EntityResultDescriptor into memento - %s : %s (%s)")
	void resolvingEntityResult(String tableAlias, String entityName, String registrationName);

	@LogMessage(level = TRACE)
	@Message(id = 160305, value = "Creating PropertyFetchDescriptor (%s : %s) for ResultSet mapping - %s")
	void creatingPropertyFetch(HbmResultSetMappingDescriptor.HbmFetchParent parent, String propertyPath, String registrationName);

	@LogMessage(level = TRACE)
	@Message(id = 160306, value = "Resolving HBM PropertyFetchDescriptor into memento - %s : %s")
	void resolvingPropertyFetch(HbmResultSetMappingDescriptor.HbmFetchParent parent, String propertyPath);

	@LogMessage(level = TRACE)
	@Message(id = 160307, value = "Resolving HBM JoinDescriptor into memento - %s : %s . %s")
	void resolvingJoin(String tableAlias, String ownerTableAlias, String propertyPath);

	@LogMessage(level = TRACE)
	@Message(id = 160308, value = "Creating CollectionResultDescriptor (%s : %s)")
	void creatingCollectionResult(String tableAlias, NavigablePath collectionPath);

	@LogMessage(level = TRACE)
	@Message(id = 160309, value = "Resolving HBM CollectionResultDescriptor into memento - %s : %s")
	void resolvingCollectionResult(String tableAlias, NavigablePath collectionPath);

	@LogMessage(level = TRACE)
	@Message(id = 160310, value = "Creating ScalarDescriptor (%s)")
	void creatingScalarResult(String columnName);

	@LogMessage(level = TRACE)
	@Message(id = 160311, value = "Resolving HBM ScalarDescriptor into memento - %s")
	void resolvingScalarResult(String columnName);

	@LogMessage(level = TRACE)
	@Message(id = 160312, value = "Creating implicit HbmResultSetMappingDescriptor for named-native-query : %s")
	void creatingImplicitHbmResultSetMapping(String registrationName);

	@LogMessage(level = TRACE)
	@Message(id = 160313, value = "Generating ScalarResultMappingMemento for JPA ColumnResult(%s) for ResultSet mapping `%s`")
	void generatingScalarResultMemento(String columnName, String mappingName);

	@LogMessage(level = TRACE)
	@Message(id = 160314, value = "Generating InstantiationResultMappingMemento for JPA ConstructorResult(%s) for ResultSet mapping `%s`")
	void generatingInstantiationResultMemento(String className, String mappingName);
}
