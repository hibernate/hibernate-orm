/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal;

import org.hibernate.HibernateException;
import org.hibernate.envers.AuditReader;

/**
 * The main contract for the creation of an {@link AuditReader} instances.
 *
 * @author Chris Cranford
 * @since 6.0
 */
public interface AuditReaderFactory {
	/**
	 * Open a {@link AuditReader}.
	 * </p>
	 * Note that AuditReader instances open their own underlying hibernate session so it is useful
	 * to cache and reuse the same AuditReader instance.
	 *
	 * @return The created AuditReader.
	 * @throws HibernateException Indicates a problem opening the audit reader.
	 */
	AuditReader openAuditReader() throws HibernateException;
}
