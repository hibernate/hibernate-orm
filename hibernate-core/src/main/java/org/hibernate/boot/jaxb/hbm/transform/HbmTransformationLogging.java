/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

import org.hibernate.boot.BootLogging;

import org.jboss.logging.Logger;

/**
 * Logging for HBM transformation
 *
 * @author Steve Ebersole
 */
public class HbmTransformationLogging {
	public static final String TRANSFORMATION_LOGGER_NAME = BootLogging.NAME + ".models.hbm-transform";
	public static final Logger TRANSFORMATION_LOGGER = Logger.getLogger( TRANSFORMATION_LOGGER_NAME );
}
