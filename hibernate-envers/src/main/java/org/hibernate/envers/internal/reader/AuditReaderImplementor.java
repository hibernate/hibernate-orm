/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
