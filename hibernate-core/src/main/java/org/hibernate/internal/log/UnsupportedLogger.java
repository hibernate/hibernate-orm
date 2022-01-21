/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.log;

import org.hibernate.cfg.AvailableSettings;

import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * Class to consolidate logging about usage of features which should
 * never be used.
 * Such features might have been introduced for practical reasons so
 * that people who really know what they want can use them, with the
 * understanding that they should find a better alternative.
 *
 * @author Sanne Grinovero
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90002001, max = 90003000 )
public interface UnsupportedLogger {

}
