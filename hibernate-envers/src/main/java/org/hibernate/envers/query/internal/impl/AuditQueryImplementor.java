/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.internal.impl;

import org.hibernate.Incubating;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.projection.AuditProjection;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@Incubating
interface AuditQueryImplementor extends AuditQuery {

	void registerProjection(final String entityName, final AuditProjection projection);

}
