/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;


/**
 * @author Steve Ebersole
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90005801, max = 90005900)
@SubSystemLogging(
		name = BytecodeEnhancementLogging.LOGGER_NAME,
		description = "Logging related to bytecode handling"
)
@Internal
public interface BytecodeEnhancementLogging extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".bytecode.enhancement";
	BytecodeEnhancementLogging LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), BytecodeEnhancementLogging.class, LOGGER_NAME  );

	// ---- trace messages ----
	@LogMessage(level = TRACE)
	@Message(id = 90005801, value = "Skipping enhancement of [%s]: it's already annotated with @EnhancementInfo")
	void skippingAlreadyAnnotated(String className);

	@LogMessage(level = TRACE)
	@Message(id = 90005802, value = "Skipping enhancement of [%s]: it's an interface")
	void skippingInterface(String className);

	@LogMessage(level = TRACE)
	@Message(id = 90005803, value = "Skipping enhancement of [%s]: it's a record")
	void skippingRecord(String className);

	@LogMessage(level = TRACE)
	@Message(id = 90005804, value = "Enhancing [%s] as Entity")
	void enhancingAsEntity(String className);

	@LogMessage(level = TRACE)
	@Message(id = 90005805, value = "Enhancing [%s] as Composite")
	void enhancingAsComposite(String className);

	@LogMessage(level = TRACE)
	@Message(id = 90005806, value = "Enhancing [%s] as MappedSuperclass")
	void enhancingAsMappedSuperclass(String className);

	@LogMessage(level = TRACE)
	@Message(id = 90005807, value = "Extended enhancement of [%s]")
	void extendedEnhancement(String className);

	@LogMessage(level = TRACE)
	@Message(id = 90005808, value = "Skipping enhancement of [%s]: not entity or composite")
	void skippingNotEntityOrComposite(String className);

	@LogMessage(level = TRACE)
	@Message(id = 90005809, value = "Weaving in PersistentAttributeInterceptable implementation on [%s]")
	void weavingPersistentAttributeInterceptable(String className);

	@LogMessage(level = TRACE)
	@Message(id = 90005810, value = "mappedBy association for field [%s.%s] is [%s.%s]")
	void mappedByAssociation(String ownerName, String fieldName, String targetEntityName, String targetFieldName);

	@LogMessage(level = TRACE)
	@Message(id = 90005811, value = "Persistent fields for entity %s: %s")
	void persistentFieldsForEntity(String entityName, String orderedFields);

	@LogMessage(level = TRACE)
	@Message(id = 90005812, value = "Found @MappedSuperclass '%s' to collectPersistenceFields")
	void foundMappedSuperclass(String superClassName);

	@LogMessage(level = TRACE)
	@Message(id = 90005813, value = "Extended enhancement: Transforming access to field [%s.%s] from method [%s.%s()]")
	void extendedTransformingFieldAccess(String ownerType, String fieldName, String methodOwner, String methodName);

	// ---- debug messages ----
	@LogMessage(level = DEBUG)
	@Message(id = 90005820, value = "Skipping re-enhancement version check for '%s' due to 'ignore'")
	void skippingReEnhancementVersionCheck(String className);

	@LogMessage(level = DEBUG)
	@Message(id = 90005821, value = "Skipping enhancement of [%s] because no field named [%s] could be found for property accessor method [%s]."
									+ " To fix this, make sure all property accessor methods have a matching field.")
	void propertyAccessorNoFieldSkip(String className, String fieldName, String methodName);

	// ---- info messages ----
	@LogMessage(level = INFO)
	@Message(id = 90005830, value = "Bidirectional association not managed for field [%s.%s]: Could not find target field in [%s]")
	void bidirectionalNotManagedCouldNotFindTargetField(String ownerName, String fieldName, String targetEntityCanonicalName);

	@LogMessage(level = INFO)
	@Message(id = 90005831, value = "Bidirectional association not managed for field [%s.%s]: @ManyToMany in java.util.Map attribute not supported ")
	void manyToManyInMapNotSupported(String ownerName, String fieldName);

	@LogMessage(level = INFO)
	@Message(id = 90005832, value = "Bidirectional association not managed for field [%s.%s]: Could not find target type")
	void bidirectionalNotManagedCouldNotFindTargetType(String ownerName, String fieldName);
}
