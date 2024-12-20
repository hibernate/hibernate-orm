/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.criteria;


/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface ExtendableCriterion {
	ExtendableCriterion add(AuditCriterion criterion);
}
