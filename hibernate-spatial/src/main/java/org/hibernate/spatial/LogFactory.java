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

import org.jboss.logging.Logger;

/**
 * A static factory for <code>Log</code>s.
 *
 * The implementation is based on the hibernate-ogm LoggerFactory class.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/14/12
 */
public class LogFactory {

	private LogFactory(){}

	/**
	 * Creates a new logger for the class that invokes this method.
	 *
	 * @return A new logger for the invoking class.
	 */
	public static Log make() {
		final Throwable t = new Throwable();
		final StackTraceElement directCaller = t.getStackTrace()[1];
		return Logger.getMessageLogger( Log.class, directCaller.getClassName() );
	}


}
