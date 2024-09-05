/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.hbm.transform;

import org.hibernate.boot.BootLogging;
import org.hibernate.boot.jaxb.JaxbLogger;

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
