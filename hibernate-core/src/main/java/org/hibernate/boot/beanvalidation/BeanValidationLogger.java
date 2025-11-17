/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.beanvalidation;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.hibernate.cfg.ValidationSettings.JAKARTA_VALIDATION_MODE;
import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Sub-system logging related to Bean Validation integration
 */
@SubSystemLogging(
		name = BeanValidationLogger.NAME,
		description = "Logging related to Bean Validation integration"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 101001, max = 101500)
@Internal
public interface BeanValidationLogger extends BasicLogger {
	String NAME = SubSystemLogging.BASE + ".beanvalidation";

	BeanValidationLogger BEAN_VALIDATION_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), BeanValidationLogger.class, NAME );

	@LogMessage(level = DEBUG)
	@Message(id = 101001, value = "Unable to acquire Jakarta Validation ValidatorFactory, skipping activation")
	void validationFactorySkipped();

	@LogMessage(level = DEBUG)
	@Message(id = 101002, value = "Skipping application of relational constraints from legacy Hibernate Validator")
	void skippingLegacyHVConstraints();

	@LogMessage(level = DEBUG)
	@Message(id = 101003, value = "ConstraintComposition type could not be determined. Assuming AND")
	void constraintCompositionTypeUnknown(@Cause Throwable ex);

	@LogMessage(level = DEBUG)
	@Message(id = 101004, value = "@NotNull was applied to attribute [%s] which is defined (at least partially) by formula(s); formula portions will be skipped")
	void notNullOnFormulaPortion(String propertyName);

	@LogMessage(level = WARN)
	@Message(id = 101005, value = "Unable to apply constraints on DDL for %s")
	void unableToApplyConstraints(String className, @Cause Exception e);

	@LogMessage(level = INFO)
	@Message(id = 101006, value = "'" + JAKARTA_VALIDATION_MODE + "' named multiple values: %s")
	void multipleValidationModes(String modes);

}
