/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.criteria;

import org.hibernate.envers.query.criteria.internal.RelatedAuditExpression;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;

/**
 * Create restrictions on an id of an entity related to an audited entity.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"JavaDoc"})
public class AuditRelatedId {
	private final PropertyNameGetter propertyNameGetter;

	public AuditRelatedId(PropertyNameGetter propertyNameGetter) {
		this.propertyNameGetter = propertyNameGetter;
	}

	/**
	 * Apply an "equal" constraint
	 */
	public AuditCriterion eq(Object id) {
		return new RelatedAuditExpression( propertyNameGetter, id, true );
	}

	/**
	 * Apply a "not equal" constraint
	 */
	public AuditCriterion ne(Object id) {
		return new RelatedAuditExpression( propertyNameGetter, id, false );
	}
}
