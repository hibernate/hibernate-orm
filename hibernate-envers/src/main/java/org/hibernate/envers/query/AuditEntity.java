/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.criteria.AuditConjunction;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.criteria.AuditDisjunction;
import org.hibernate.envers.query.criteria.AuditId;
import org.hibernate.envers.query.criteria.AuditProperty;
import org.hibernate.envers.query.criteria.AuditRelatedId;
import org.hibernate.envers.query.criteria.internal.LogicalAuditExpression;
import org.hibernate.envers.query.criteria.internal.NotAuditExpression;
import org.hibernate.envers.query.internal.property.EntityPropertyName;
import org.hibernate.envers.query.internal.property.RevisionNumberPropertyName;
import org.hibernate.envers.query.internal.property.RevisionPropertyPropertyName;
import org.hibernate.envers.query.internal.property.RevisionTypePropertyName;

/**
 * TODO: ilike
 *
 * @author Adam Warski (adam at warski dot org)
 *
 * @see org.hibernate.criterion.Restrictions
 */
@SuppressWarnings({"JavaDoc"})
public class AuditEntity {
	private AuditEntity() {
	}

	public static AuditId id() {
		return new AuditId();
	}

	/**
	 * Create restrictions, projections and specify order for a property of an audited entity.
	 *
	 * @param propertyName Name of the property.
	 */
	public static AuditProperty<Object> property(String propertyName) {
		return new AuditProperty<Object>( new EntityPropertyName( propertyName ) );
	}

	/**
	 * Create restrictions, projections and specify order for the revision number, corresponding to an
	 * audited entity.
	 */
	public static AuditProperty<Number> revisionNumber() {
		return new AuditProperty<Number>( new RevisionNumberPropertyName() );
	}

	/**
	 * Create restrictions, projections and specify order for a property of the revision entity,
	 * corresponding to an audited entity.
	 *
	 * @param propertyName Name of the property.
	 */
	public static AuditProperty<Object> revisionProperty(String propertyName) {
		return new AuditProperty<Object>( new RevisionPropertyPropertyName( propertyName ) );
	}

	/**
	 * Create restrictions, projections and specify order for the revision type, corresponding to an
	 * audited entity.
	 */
	public static AuditProperty<RevisionType> revisionType() {
		return new AuditProperty<RevisionType>( new RevisionTypePropertyName() );
	}

	/**
	 * Create restrictions on an id of a related entity.
	 *
	 * @param propertyName Name of the property, which is the relation.
	 */
	public static AuditRelatedId relatedId(String propertyName) {
		return new AuditRelatedId( new EntityPropertyName( propertyName ) );
	}

	/**
	 * Return the conjuction of two criterions.
	 */
	public static AuditCriterion and(AuditCriterion lhs, AuditCriterion rhs) {
		return new LogicalAuditExpression( lhs, rhs, "and" );
	}

	/**
	 * Return the disjuction of two criterions.
	 */
	public static AuditCriterion or(AuditCriterion lhs, AuditCriterion rhs) {
		return new LogicalAuditExpression( lhs, rhs, "or" );
	}

	/**
	 * Return the negation of a criterion.
	 */
	public static AuditCriterion not(AuditCriterion expression) {
		return new NotAuditExpression( expression );
	}

	/**
	 * Group criterions together in a single conjunction (A and B and C...).
	 */
	public static AuditConjunction conjunction() {
		return new AuditConjunction();
	}

	/**
	 * Group criterions together in a single disjunction (A or B or C...).
	 */
	public static AuditDisjunction disjunction() {
		return new AuditDisjunction();
	}
}
