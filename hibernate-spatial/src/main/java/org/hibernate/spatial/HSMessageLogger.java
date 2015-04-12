/*
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for spatial (geographic) data.
 *
 * Copyright © 2007-2013 Geovise BVBA
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.hibernate.spatial;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.hibernate.internal.CoreMessageLogger;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * The logger interface for the Hibernate Spatial module.
 *
 * @author Karel Maesen, Geovise BVBA
 */
@MessageLogger(projectCode = "HHH")
public interface HSMessageLogger extends BasicLogger {
	/**
	 * Message indicating that user attempted to use the deprecated ValidTimeAuditStrategy
	 */
//	@LogMessage(level = WARN)
//	@Message(value = "ValidTimeAuditStrategy is deprecated, please use ValidityAuditStrategy instead", id = 25001)
//	void voidvalidTimeAuditStrategyDeprecated();

}