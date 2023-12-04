/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize;

import org.hibernate.Internal;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.INFO;

/**
 * todo : find the proper min/max id range
 *
 * @author Steve Ebersole
 */
@Internal
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 999901, max = 999999 )
public interface ModelCategorizationLogging extends BasicLogger {
	String NAME = "org.hibernate.models.orm";

	Logger MODEL_CATEGORIZATION_LOGGER = Logger.getLogger( NAME );
	ModelCategorizationLogging MODEL_CATEGORIZATION_MSG_LOGGER = Logger.getMessageLogger( ModelCategorizationLogging.class, NAME );

	@LogMessage(level = INFO)
	@Message( id = 999901, value = "Entity `%s` used both @DynamicInsert and @SQLInsert" )
	void dynamicAndCustomInsert(String entityName);

	@LogMessage(level = INFO)
	@Message( id = 999902, value = "Entity `%s` used both @DynamicUpdate and @SQLUpdate" )
	void dynamicAndCustomUpdate(String entityName);
}
