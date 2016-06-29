/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.spi;

import org.hibernate.envers.boot.AuditMetadata;
import org.hibernate.envers.boot.AuditService;

/**
 * Internal API for AuditService exposing the {@link AuditMetadata}.
 *
 * @author Chris Cranford
 * @since 6.0
 */
public interface AuditServiceImplementor extends AuditService {
	AuditMetadata getMetadata();
}
