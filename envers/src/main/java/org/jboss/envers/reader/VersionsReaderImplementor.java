/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.reader;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.Session;
import org.jboss.envers.VersionsReader;

/**
 * An interface exposed by a VersionsReader to library-facing classes.
 * @author Adam Warski (adam at warski dot org)
 */
public interface VersionsReaderImplementor extends VersionsReader {
    SessionImplementor getSessionImplementor();
    Session getSession();
    FirstLevelCache getFirstLevelCache();
}
