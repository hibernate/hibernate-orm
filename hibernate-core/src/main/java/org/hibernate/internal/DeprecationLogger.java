/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.internal;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.INFO;

/**
 * Class to consolidate logging about usage of deprecated features.
 *
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90000001, max = 90001000 )
public interface DeprecationLogger {
	public static final DeprecationLogger DEPRECATION_LOGGER = Logger.getMessageLogger(
			DeprecationLogger.class,
			"org.hibernate.orm.deprecation"
	);

	/**
	 * Log about usage of deprecated Scanner setting
	 */
	@LogMessage( level = INFO )
	@Message(
			value = "Found usage of deprecated setting for specifying Scanner [hibernate.ejb.resource_scanner]; " +
					"use [hibernate.archive.scanner] instead",
			id = 90000001
	)
	public void logDeprecatedScannerSetting();
}
