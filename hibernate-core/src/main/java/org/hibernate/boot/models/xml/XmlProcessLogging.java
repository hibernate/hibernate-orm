/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
