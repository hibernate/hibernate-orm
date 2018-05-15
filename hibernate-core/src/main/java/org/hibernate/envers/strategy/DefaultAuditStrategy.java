/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.strategy;

import org.hibernate.annotations.Remove;

/**
 * Default strategy is to simply persist the audit data.
 *
 * @author Adam Warski
 * @author Stephanie Pau
 * @author Chris Cranford
 *
 * @deprecated (since 5.4), use {@link org.hibernate.envers.strategy.internal.DefaultAuditStrategy} instead.
 */
@Deprecated
@Remove
public class DefaultAuditStrategy extends org.hibernate.envers.strategy.internal.DefaultAuditStrategy {


}
