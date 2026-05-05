/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * API for querying the history of
 * {@linkplain org.hibernate.annotations.Audited audited} entities.
 * <p>
 * Audited entities are transparently versioned: every insert, update,
 * and delete is recorded in a companion audit table. Point-in-time
 * reads are available via
 * {@link org.hibernate.SessionBuilder#atChangeset(Object)
 * atTransaction()} sessions, while the {@link org.hibernate.audit.AuditLog}
 * interface provides programmatic access to revision history,
 * modification types, and cross-entity change queries.
 * <p>
 * This package also contains the base classes and contracts for
 * defining custom
 * {@linkplain org.hibernate.annotations.ChangesetEntity revision
 * entities} and {@linkplain org.hibernate.audit.ChangesetListener
 * revision listeners}.
 *
 * @see org.hibernate.annotations.Audited
 * @see org.hibernate.annotations.ChangesetEntity
 * @see org.hibernate.audit.AuditLog
 * @see org.hibernate.audit.AuditLogFactory
 */
@Incubating
package org.hibernate.audit;

import org.hibernate.Incubating;
