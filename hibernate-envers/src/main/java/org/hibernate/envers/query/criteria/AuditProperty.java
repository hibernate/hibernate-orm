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

package org.hibernate.envers.query.criteria;

import org.hibernate.envers.query.property.PropertyNameGetter;
import org.hibernate.envers.query.order.AuditOrder;
import org.hibernate.envers.query.order.PropertyAuditOrder;
import org.hibernate.envers.query.projection.AuditProjection;
import org.hibernate.envers.query.projection.PropertyAuditProjection;
import org.hibernate.envers.tools.Triple;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.criterion.MatchMode;

import java.util.Collection;

/**
 * Create restrictions, projections and specify order for a property of an audited entity.
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"JavaDoc"})
public class AuditProperty<T> implements AuditProjection {
    private final PropertyNameGetter propertyNameGetter;

    public AuditProperty(PropertyNameGetter propertyNameGetter) {
        this.propertyNameGetter = propertyNameGetter;
    }

    /**
	 * Apply an "equal" constraint
	 */
	public AuditCriterion eq(T value) {
		return new SimpleAuditExpression(propertyNameGetter, value, "=");
	}

    /**
	 * Apply a "not equal" constraint
	 */
	public AuditCriterion ne(T value) {
		return new SimpleAuditExpression(propertyNameGetter, value, "<>");
	}

    /**
	 * Apply a "like" constraint
	 */
	public AuditCriterion like(T value) {
		return new SimpleAuditExpression(propertyNameGetter, value, " like ");
	}

    /**
	 * Apply a "like" constraint
	 */
	public AuditCriterion like(String value, MatchMode matchMode) {
		return new SimpleAuditExpression(propertyNameGetter, matchMode.toMatchString(value), " like " );
	}

    /**
	 * Apply a "greater than" constraint
	 */
	public AuditCriterion gt(T value) {
		return new SimpleAuditExpression(propertyNameGetter, value, ">");
	}

    /**
	 * Apply a "less than" constraint
	 */
	public AuditCriterion lt(T value) {
		return new SimpleAuditExpression(propertyNameGetter, value, "<");
	}

    /**
	 * Apply a "less than or equal" constraint
	 */
	public AuditCriterion le(T value) {
		return new SimpleAuditExpression(propertyNameGetter, value, "<=");
	}

    /**
	 * Apply a "greater than or equal" constraint
	 */
	public AuditCriterion ge(T value) {
		return new SimpleAuditExpression(propertyNameGetter, value, ">=");
	}

    /**
	 * Apply a "between" constraint
	 */
	public AuditCriterion between(T lo, T hi) {
		return new BetweenAuditExpression(propertyNameGetter, lo, hi);
	}

    /**
	 * Apply an "in" constraint
	 */
	public AuditCriterion in(T[] values) {
		return new InAuditExpression(propertyNameGetter, values);
	}

    /**
	 * Apply an "in" constraint
	 */
	public AuditCriterion in(Collection values) {
		return new InAuditExpression(propertyNameGetter, values.toArray());
	}

    /**
	 * Apply an "is null" constraint
	 */
	public AuditCriterion isNull() {
		return new NullAuditExpression(propertyNameGetter);
	}

    /**
	 * Apply an "equal" constraint to another property
	 */
	public AuditCriterion eqProperty(String otherPropertyName) {
		return new PropertyAuditExpression(propertyNameGetter, otherPropertyName, "=");
	}

    /**
	 * Apply a "not equal" constraint to another property
	 */
	public AuditCriterion neProperty(String otherPropertyName) {
		return new PropertyAuditExpression(propertyNameGetter, otherPropertyName, "<>");
	}

    /**
	 * Apply a "less than" constraint to another property
	 */
	public AuditCriterion ltProperty(String otherPropertyName) {
		return new PropertyAuditExpression(propertyNameGetter, otherPropertyName, "<");
	}

    /**
	 * Apply a "less than or equal" constraint to another property
	 */
	public AuditCriterion leProperty(String otherPropertyName) {
		return new PropertyAuditExpression(propertyNameGetter, otherPropertyName, "<=");
	}

    /**
	 * Apply a "greater than" constraint to another property
	 */
	public AuditCriterion gtProperty(String otherPropertyName) {
		return new PropertyAuditExpression(propertyNameGetter, otherPropertyName, ">");
	}

    /**
	 * Apply a "greater than or equal" constraint to another property
	 */
	public AuditCriterion geProperty(String otherPropertyName) {
		return new PropertyAuditExpression(propertyNameGetter, otherPropertyName, ">=");
	}

    /**
	 * Apply an "is not null" constraint to the another property
	 */
	public AuditCriterion isNotNull() {
		return new NotNullAuditExpression(propertyNameGetter);
	}

    /**
     * Apply a "maximalize" constraint, with the ability to specify further constraints on the maximized
     * property
     */
    public AggregatedAuditExpression maximize() {
        return new AggregatedAuditExpression(propertyNameGetter,
                AggregatedAuditExpression.AggregatedMode.MAX);
    }

    /**
     * Apply a "minimize" constraint, with the ability to specify further constraints on the minimized
     * property
     */
    public AggregatedAuditExpression minimize() {
        return new AggregatedAuditExpression(propertyNameGetter,
                AggregatedAuditExpression.AggregatedMode.MIN);
    }

    // Projections

    /**
     * Projection on the maximum value
     */
    public AuditProjection max() {
        return new PropertyAuditProjection(propertyNameGetter, "max", false);
    }

    /**
     * Projection on the minimum value
     */
    public AuditProjection min() {
        return new PropertyAuditProjection(propertyNameGetter, "min", false);
    }

    /**
     * Projection counting the values
     */
    public AuditProjection count() {
        return new PropertyAuditProjection(propertyNameGetter, "count", false);
    }

    /**
     * Projection counting distinct values
     */
    public AuditProjection countDistinct() {
        return new PropertyAuditProjection(propertyNameGetter, "count", true);
    }

    /**
     * Projection on distinct values
     */
    public AuditProjection distinct() {
        return new PropertyAuditProjection(propertyNameGetter, null, true);
    }

    /**
     * Projection using a custom function
     */
    public AuditProjection function(String functionName) {
        return new PropertyAuditProjection(propertyNameGetter, functionName, false);
    }

    // Projection on this property

    public Triple<String, String, Boolean> getData(AuditConfiguration auditCfg) {
        return Triple.make(null, propertyNameGetter.get(auditCfg), false);
    }

    // Order

    /**
     * Sort the results by the property in ascending order
     */
    public AuditOrder asc() {
        return new PropertyAuditOrder(propertyNameGetter, true);
    }

    /**
     * Sort the results by the property in descending order
     */
    public AuditOrder desc() {
        return new PropertyAuditOrder(propertyNameGetter, false);
    }
}
