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
package org.jboss.envers.query;

import java.util.Collection;

import org.jboss.envers.query.criteria.AggregatedFieldVersionsExpression;
import org.jboss.envers.query.criteria.BetweenVersionsExpression;
import org.jboss.envers.query.criteria.IdentifierEqVersionsExpression;
import org.jboss.envers.query.criteria.InVersionsExpression;
import org.jboss.envers.query.criteria.LogicalVersionsExpression;
import org.jboss.envers.query.criteria.NotNullVersionsExpression;
import org.jboss.envers.query.criteria.NotVersionsExpression;
import org.jboss.envers.query.criteria.NullVersionsExpression;
import org.jboss.envers.query.criteria.PropertyVersionsExpression;
import org.jboss.envers.query.criteria.RelatedVersionsExpression;
import org.jboss.envers.query.criteria.SimpleVersionsExpression;
import org.jboss.envers.query.criteria.VersionsConjunction;
import org.jboss.envers.query.criteria.VersionsCriterion;
import org.jboss.envers.query.criteria.VersionsDisjunction;

import org.hibernate.criterion.MatchMode;

/**
 * TODO: ilike
 * @author Adam Warski (adam at warski dot org)
 * @see org.hibernate.criterion.Restrictions
 */
@SuppressWarnings({"JavaDoc"})
public class VersionsRestrictions {
    private VersionsRestrictions() { }

	/**
	 * Apply an "equal" constraint to the identifier property.
	 */
	public static VersionsCriterion idEq(Object value) {
		return new IdentifierEqVersionsExpression(value);
	}
    
    /**
	 * Apply an "equal" constraint to the named property
	 */
	public static VersionsCriterion eq(String propertyName, Object value) {
		return new SimpleVersionsExpression(propertyName, value, "=");
	}

    /**
	 * Apply a "not equal" constraint to the named property
	 */
	public static VersionsCriterion ne(String propertyName, Object value) {
		return new SimpleVersionsExpression(propertyName, value, "<>");
	}

    /**
	 * Apply an "equal" constraint on an id of a related entity
	 */
	public static VersionsCriterion relatedIdEq(String propertyName, Object id) {
		return new RelatedVersionsExpression(propertyName, id, true);
	}

    /**
	 * Apply a "not equal" constraint to the named property
	 */
	public static VersionsCriterion relatedIdNe(String propertyName, Object id) {
		return new RelatedVersionsExpression(propertyName, id, false);
	}

    /**
	 * Apply a "like" constraint to the named property
	 */
	public static VersionsCriterion like(String propertyName, Object value) {
		return new SimpleVersionsExpression(propertyName, value, " like ");
	}

    /**
	 * Apply a "like" constraint to the named property
	 */
	public static VersionsCriterion like(String propertyName, String value, MatchMode matchMode) {
		return new SimpleVersionsExpression(propertyName, matchMode.toMatchString(value), " like " );
	}

    /**
	 * Apply a "greater than" constraint to the named property
	 */
	public static VersionsCriterion gt(String propertyName, Object value) {
		return new SimpleVersionsExpression(propertyName, value, ">");
	}

    /**
	 * Apply a "less than" constraint to the named property
	 */
	public static VersionsCriterion lt(String propertyName, Object value) {
		return new SimpleVersionsExpression(propertyName, value, "<");
	}

    /**
	 * Apply a "less than or equal" constraint to the named property
	 */
	public static VersionsCriterion le(String propertyName, Object value) {
		return new SimpleVersionsExpression(propertyName, value, "<=");
	}

    /**
	 * Apply a "greater than or equal" constraint to the named property
	 */
	public static VersionsCriterion ge(String propertyName, Object value) {
		return new SimpleVersionsExpression(propertyName, value, ">=");
	}

    /**
	 * Apply a "between" constraint to the named property
	 */
	public static VersionsCriterion between(String propertyName, Object lo, Object hi) {
		return new BetweenVersionsExpression(propertyName, lo, hi);
	}

    /**
	 * Apply an "in" constraint to the named property
	 */
	public static VersionsCriterion in(String propertyName, Object[] values) {
		return new InVersionsExpression(propertyName, values);
	}

    /**
	 * Apply an "in" constraint to the named property
	 */
	public static VersionsCriterion in(String propertyName, Collection values) {
		return new InVersionsExpression(propertyName, values.toArray());
	}

    /**
	 * Apply an "is null" constraint to the named property
	 */
	public static VersionsCriterion isNull(String propertyName) {
		return new NullVersionsExpression(propertyName);
	}

    /**
	 * Apply an "equal" constraint to two properties
	 */
	public static VersionsCriterion eqProperty(String propertyName, String otherPropertyName) {
		return new PropertyVersionsExpression(propertyName, otherPropertyName, "=");
	}

    /**
	 * Apply a "not equal" constraint to two properties
	 */
	public static VersionsCriterion neProperty(String propertyName, String otherPropertyName) {
		return new PropertyVersionsExpression(propertyName, otherPropertyName, "<>");
	}
    
    /**
	 * Apply a "less than" constraint to two properties
	 */
	public static VersionsCriterion ltProperty(String propertyName, String otherPropertyName) {
		return new PropertyVersionsExpression(propertyName, otherPropertyName, "<");
	}

    /**
	 * Apply a "less than or equal" constraint to two properties
	 */
	public static VersionsCriterion leProperty(String propertyName, String otherPropertyName) {
		return new PropertyVersionsExpression(propertyName, otherPropertyName, "<=");
	}

    /**
	 * Apply a "greater than" constraint to two properties
	 */
	public static VersionsCriterion gtProperty(String propertyName, String otherPropertyName) {
		return new PropertyVersionsExpression(propertyName, otherPropertyName, ">");
	}

    /**
	 * Apply a "greater than or equal" constraint to two properties
	 */
	public static VersionsCriterion geProperty(String propertyName, String otherPropertyName) {
		return new PropertyVersionsExpression(propertyName, otherPropertyName, ">=");
	}

    /**
	 * Apply an "is not null" constraint to the named property
	 */
	public static VersionsCriterion isNotNull(String propertyName) {
		return new NotNullVersionsExpression(propertyName);
	}

    /**
	 * Return the conjuction of two expressions
	 */
	public static VersionsCriterion and(VersionsCriterion lhs, VersionsCriterion rhs) {
		return new LogicalVersionsExpression(lhs, rhs, "and");
	}

    /**
	 * Return the disjuction of two expressions
	 */
	public static VersionsCriterion or(VersionsCriterion lhs, VersionsCriterion rhs) {
		return new LogicalVersionsExpression(lhs, rhs, "or");
	}

    /**
	 * Return the negation of an expression
	 */
	public static VersionsCriterion not(VersionsCriterion expression) {
		return new NotVersionsExpression(expression);
	}

	/**
	 * Group expressions together in a single conjunction (A and B and C...)
	 */
	public static VersionsConjunction conjunction() {
		return new VersionsConjunction();
	}

	/**
	 * Group expressions together in a single disjunction (A or B or C...)
	 */
	public static VersionsDisjunction disjunction() {
		return new VersionsDisjunction();
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
