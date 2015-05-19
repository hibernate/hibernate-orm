/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.reader;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.AuditReader;

/**
 * An interface exposed by a VersionsReader to library-facing classes.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public interface AuditReaderImplementor extends AuditReader {
	SessionImplementor getSessionImplementor();

	Session getSession();

	FirstLevelCache getFirstLevelCache();
}
