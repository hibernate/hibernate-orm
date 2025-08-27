/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.strategy;

/**
 * Default strategy is to simply persist the audit data.
 *
 * @deprecated use {@link org.hibernate.envers.strategy.internal.DefaultAuditStrategy} instead.
 *
 * @author Adam Warski
 * @author Stephanie Pau
 * @author Chris Cranford
 */
@Deprecated(since = "5.4")
public class DefaultAuditStrategy extends org.hibernate.envers.strategy.internal.DefaultAuditStrategy {

}
