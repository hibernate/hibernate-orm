/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.query;

import org.hibernate.criterion.Restrictions;
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
 * @see Restrictions
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
