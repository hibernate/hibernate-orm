/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.criteria;

import org.hibernate.envers.query.criteria.internal.RelatedAuditEqualityExpression;
import org.hibernate.envers.query.criteria.internal.RelatedAuditInExpression;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;

/**
 * Create restrictions on an id of an entity related to an audited entity.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class AuditRelatedId {
	private final String alias;
	private final PropertyNameGetter propertyNameGetter;

	public AuditRelatedId(String alias, PropertyNameGetter propertyNameGetter) {
		this.alias = alias;
		this.propertyNameGetter = propertyNameGetter;
	}

	/**
	 * Applies an "equals" criteria predicate.
	 *
	 * @param id the value to test equality with
	 * @return the criterion.
	 */
	public AuditCriterion eq(Object id) {
		return new RelatedAuditEqualityExpression( alias, propertyNameGetter, id, true );
	}

	/**
	 * Applies a "not equals" criteria predicate.
	 *
	 * @param id the value to test inequality with
	 * @return the criterion
	 */
	public AuditCriterion ne(Object id) {
		return new RelatedAuditEqualityExpression( alias, propertyNameGetter, id, false );
	}

	/**
	 * Applies an "in" criteria predicate.
	 *
	 * @param values the values to test with
*      @return the criterion
	 */
	public AuditCriterion in(Object[] values) {
		return new RelatedAuditInExpression( alias, propertyNameGetter, values );
	}
}
