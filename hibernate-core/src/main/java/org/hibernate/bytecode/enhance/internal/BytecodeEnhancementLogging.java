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
@ValidIdRange(min = 90009001, max = 90009900)
@SubSystemLogging(
		name = BytecodeEnhancementLogging.LOGGER_NAME,
		description = "Logging related to bytecode handling"
)
@Internal
public interface BytecodeEnhancementLogging extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".bytecode.enhancement";
	BytecodeEnhancementLogging ENHANCEMENT_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), BytecodeEnhancementLogging.class, LOGGER_NAME  );

	// ---- trace messages ----
	@LogMessage(level = TRACE)
	@Message(id = 90009001, value = "Skipping enhancement of [%s]: it's already annotated with @EnhancementInfo")
	void skippingAlreadyAnnotated(String className);

	@LogMessage(level = TRACE)
	@Message(id = 90009002, value = "Skipping enhancement of [%s]: it's an interface")
	void skippingInterface(String className);

	@LogMessage(level = TRACE)
	@Message(id = 90009003, value = "Skipping enhancement of [%s]: it's a record")
	void skippingRecord(String className);

	@LogMessage(level = TRACE)
	@Message(id = 90009004, value = "Enhancing [%s] as Entity")
	void enhancingAsEntity(String className);

	@LogMessage(level = TRACE)
	@Message(id = 90009005, value = "Enhancing [%s] as Composite")
	void enhancingAsComposite(String className);

	@LogMessage(level = TRACE)
	@Message(id = 90009006, value = "Enhancing [%s] as MappedSuperclass")
	void enhancingAsMappedSuperclass(String className);

	@LogMessage(level = TRACE)
	@Message(id = 90009007, value = "Extended enhancement of [%s]")
	void extendedEnhancement(String className);

	@LogMessage(level = TRACE)
	@Message(id = 90009008, value = "Skipping enhancement of [%s]: not entity or composite")
	void skippingNotEntityOrComposite(String className);

	@LogMessage(level = TRACE)
	@Message(id = 90009009, value = "Weaving in PersistentAttributeInterceptable implementation on [%s]")
	void weavingPersistentAttributeInterceptable(String className);

	@LogMessage(level = TRACE)
	@Message(id = 90009010, value = "mappedBy association for field [%s.%s] is [%s.%s]")
	void mappedByAssociation(String ownerName, String fieldName, String targetEntityName, String targetFieldName);

	@LogMessage(level = TRACE)
	@Message(id = 90009011, value = "Persistent fields for entity %s: %s")
	void persistentFieldsForEntity(String entityName, String orderedFields);

	@LogMessage(level = TRACE)
	@Message(id = 90009012, value = "Found @MappedSuperclass '%s' to collectPersistenceFields")
	void foundMappedSuperclass(String superClassName);

	@LogMessage(level = TRACE)
	@Message(id = 90009013, value = "Extended enhancement: Transforming access to field [%s.%s] from method [%s.%s()]")
	void extendedTransformingFieldAccess(String ownerType, String fieldName, String methodOwner, String methodName);

	// ---- debug messages ----
	@LogMessage(level = DEBUG)
	@Message(id = 90009020, value = "Skipping re-enhancement version check for '%s' due to 'ignore'")
	void skippingReEnhancementVersionCheck(String className);

	@LogMessage(level = DEBUG)
	@Message(id = 90009021, value = "Skipping enhancement of [%s] because no field named [%s] could be found for property accessor method [%s]."
									+ " To fix this, make sure all property accessor methods have a matching field.")
	void propertyAccessorNoFieldSkip(String className, String fieldName, String methodName);

	// ---- info messages ----
	@LogMessage(level = INFO)
	@Message(id = 90009030, value = "Bidirectional association not managed for field [%s.%s]: Could not find target field in [%s]")
	void bidirectionalNotManagedCouldNotFindTargetField(String ownerName, String fieldName, String targetEntityCanonicalName);

	@LogMessage(level = INFO)
	@Message(id = 90009031, value = "Bidirectional association not managed for field [%s.%s]: @ManyToMany in java.util.Map attribute not supported ")
	void manyToManyInMapNotSupported(String ownerName, String fieldName);

	@LogMessage(level = INFO)
	@Message(id = 90009032, value = "Bidirectional association not managed for field [%s.%s]: Could not find target type")
	void bidirectionalNotManagedCouldNotFindTargetType(String ownerName, String fieldName);
}
