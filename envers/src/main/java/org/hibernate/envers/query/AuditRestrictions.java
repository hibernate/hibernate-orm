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

import java.util.Collection;

import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.criteria.*;

import org.hibernate.criterion.MatchMode;

/**
 * TODO: ilike
 * @author Adam Warski (adam at warski dot org)
 * @see org.hibernate.criterion.Restrictions
 */
@SuppressWarnings({"JavaDoc"})
public class AuditRestrictions {
    private AuditRestrictions() { }

	/**
	 * Apply an "equal" constraint to the identifier property.
	 */
	public static AuditCriterion idEq(Object value) {
		return new IdentifierEqVersionsExpression(value);
	}
    
    /**
	 * Apply an "equal" constraint to the named property
	 */
	public static AuditCriterion eq(String propertyName, Object value) {
		return new SimpleVersionsExpression(propertyName, value, "=");
	}

    /**
	 * Apply a "not equal" constraint to the named property
	 */
	public static AuditCriterion ne(String propertyName, Object value) {
		return new SimpleVersionsExpression(propertyName, value, "<>");
	}

    /**
	 * Apply an "equal" constraint on an id of a related entity
	 */
	public static AuditCriterion relatedIdEq(String propertyName, Object id) {
		return new RelatedVersionsExpression(propertyName, id, true);
	}

    /**
	 * Apply a "not equal" constraint to the named property
	 */
	public static AuditCriterion relatedIdNe(String propertyName, Object id) {
		return new RelatedVersionsExpression(propertyName, id, false);
	}

    /**
	 * Apply a "like" constraint to the named property
	 */
	public static AuditCriterion like(String propertyName, Object value) {
		return new SimpleVersionsExpression(propertyName, value, " like ");
	}

    /**
	 * Apply a "like" constraint to the named property
	 */
	public static AuditCriterion like(String propertyName, String value, MatchMode matchMode) {
		return new SimpleVersionsExpression(propertyName, matchMode.toMatchString(value), " like " );
	}

    /**
	 * Apply a "greater than" constraint to the named property
	 */
	public static AuditCriterion gt(String propertyName, Object value) {
		return new SimpleVersionsExpression(propertyName, value, ">");
	}

    /**
	 * Apply a "less than" constraint to the named property
	 */
	public static AuditCriterion lt(String propertyName, Object value) {
		return new SimpleVersionsExpression(propertyName, value, "<");
	}

    /**
	 * Apply a "less than or equal" constraint to the named property
	 */
	public static AuditCriterion le(String propertyName, Object value) {
		return new SimpleVersionsExpression(propertyName, value, "<=");
	}

    /**
	 * Apply a "greater than or equal" constraint to the named property
	 */
	public static AuditCriterion ge(String propertyName, Object value) {
		return new SimpleVersionsExpression(propertyName, value, ">=");
	}

    /**
	 * Apply a "between" constraint to the named property
	 */
	public static AuditCriterion between(String propertyName, Object lo, Object hi) {
		return new BetweenVersionsExpression(propertyName, lo, hi);
	}

    /**
	 * Apply an "in" constraint to the named property
	 */
	public static AuditCriterion in(String propertyName, Object[] values) {
		return new InVersionsExpression(propertyName, values);
	}

    /**
	 * Apply an "in" constraint to the named property
	 */
	public static AuditCriterion in(String propertyName, Collection values) {
		return new InVersionsExpression(propertyName, values.toArray());
	}

    /**
	 * Apply an "is null" constraint to the named property
	 */
	public static AuditCriterion isNull(String propertyName) {
		return new NullVersionsExpression(propertyName);
	}

    /**
	 * Apply an "equal" constraint to two properties
	 */
	public static AuditCriterion eqProperty(String propertyName, String otherPropertyName) {
		return new PropertyVersionsExpression(propertyName, otherPropertyName, "=");
	}

    /**
	 * Apply a "not equal" constraint to two properties
	 */
	public static AuditCriterion neProperty(String propertyName, String otherPropertyName) {
		return new PropertyVersionsExpression(propertyName, otherPropertyName, "<>");
	}
    
    /**
	 * Apply a "less than" constraint to two properties
	 */
	public static AuditCriterion ltProperty(String propertyName, String otherPropertyName) {
		return new PropertyVersionsExpression(propertyName, otherPropertyName, "<");
	}

    /**
	 * Apply a "less than or equal" constraint to two properties
	 */
	public static AuditCriterion leProperty(String propertyName, String otherPropertyName) {
		return new PropertyVersionsExpression(propertyName, otherPropertyName, "<=");
	}

    /**
	 * Apply a "greater than" constraint to two properties
	 */
	public static AuditCriterion gtProperty(String propertyName, String otherPropertyName) {
		return new PropertyVersionsExpression(propertyName, otherPropertyName, ">");
	}

    /**
	 * Apply a "greater than or equal" constraint to two properties
	 */
	public static AuditCriterion geProperty(String propertyName, String otherPropertyName) {
		return new PropertyVersionsExpression(propertyName, otherPropertyName, ">=");
	}

    /**
	 * Apply an "is not null" constraint to the named property
	 */
	public static AuditCriterion isNotNull(String propertyName) {
		return new NotNullVersionsExpression(propertyName);
	}

    /**
	 * Return the conjuction of two expressions
	 */
	public static AuditCriterion and(AuditCriterion lhs, AuditCriterion rhs) {
		return new LogicalVersionsExpression(lhs, rhs, "and");
	}

    /**
	 * Return the disjuction of two expressions
	 */
	public static AuditCriterion or(AuditCriterion lhs, AuditCriterion rhs) {
		return new LogicalVersionsExpression(lhs, rhs, "or");
	}

    /**
	 * Return the negation of an expression
	 */
	public static AuditCriterion not(AuditCriterion expression) {
		return new NotVersionsExpression(expression);
	}

	/**
	 * Group expressions together in a single conjunction (A and B and C...)
	 */
	public static AuditConjunction conjunction() {
		return new AuditConjunction();
	}

	/**
	 * Group expressions together in a single disjunction (A or B or C...)
	 */
	public static AuditDisjunction disjunction() {
		return new AuditDisjunction();
	}

    /**
     * Apply a "maximalize property" constraint.
     */
    public static AggregatedFieldVersionsExpression maximizeProperty(String propertyName) {
        return new AggregatedFieldVersionsExpression(propertyName,
                AggregatedFieldVersionsExpression.AggregatedMode.MAX);
    }

    /**
     * Apply a "minimize property" constraint.
     */
    public static AggregatedFieldVersionsExpression minimizeProperty(String propertyName) {
        return new AggregatedFieldVersionsExpression(propertyName,
                AggregatedFieldVersionsExpression.AggregatedMode.MIN);
    }
}
