/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml;

import org.hibernate.Internal;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@Internal
public interface XmlProcessLogging {
	String NAME = "org.hibernate.orm.boot.models.xml";

	Logger XML_PROCESS_LOGGER = Logger.getLogger( NAME );
}
