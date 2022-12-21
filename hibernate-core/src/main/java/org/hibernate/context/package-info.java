/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Contains SPIs which define:
 * <ul>
 * <li>the {@linkplain org.hibernate.context.spi.CurrentSessionContext notion}
 *     of a context-bound or "current" session, and
 * <li>the {@linkplain org.hibernate.context.spi.CurrentTenantIdentifierResolver
 *     notion} of a "current" tenant id.
 * </ul>
 */
package org.hibernate.context;
