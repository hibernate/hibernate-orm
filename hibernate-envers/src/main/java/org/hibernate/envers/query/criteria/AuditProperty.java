/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.criteria;

import java.util.Collection;

import org.hibernate.criterion.MatchMode;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.tools.Triple;
import org.hibernate.envers.query.criteria.internal.BetweenAuditExpression;
import org.hibernate.envers.query.criteria.internal.IlikeAuditExpression;
import org.hibernate.envers.query.criteria.internal.InAuditExpression;
import org.hibernate.envers.query.criteria.internal.NotNullAuditExpression;
import org.hibernate.envers.query.criteria.internal.NullAuditExpression;
import org.hibernate.envers.query.criteria.internal.PropertyAuditExpression;
import org.hibernate.envers.query.criteria.internal.SimpleAuditExpression;
import org.hibernate.envers.query.internal.property.ModifiedFlagPropertyName;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;
import org.hibernate.envers.query.order.AuditOrder;
import org.hibernate.envers.query.order.internal.PropertyAuditOrder;
import org.hibernate.envers.query.projection.AuditProjection;
import org.hibernate.envers.query.projection.internal.PropertyAuditProjection;

/**
 * Create restrictions, projections and specify order for a property of an audited entity.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@SuppressWarnings({"JavaDoc"})
public class AuditProperty<T> implements AuditProjection {
	private final PropertyNameGetter propertyNameGetter;

	public AuditProperty(PropertyNameGetter propertyNameGetter) {
		this.propertyNameGetter = propertyNameGetter;
	}

	public AuditCriterion hasChanged() {
		return new SimpleAuditExpression( new ModifiedFlagPropertyName( propertyNameGetter ), true, "=" );
	}

	public AuditCriterion hasNotChanged() {
		return new SimpleAuditExpression( new ModifiedFlagPropertyName( propertyNameGetter ), false, "=" );
	}

	/**
	 * Apply an "equal" constraint
	 */
	public AuditCriterion eq(T value) {
		return new SimpleAuditExpression( propertyNameGetter, value, "=" );
	}

	/**
	 * Apply a "not equal" constraint
	 */
	public AuditCriterion ne(T value) {
		return new SimpleAuditExpression( propertyNameGetter, value, "<>" );
	}

	/**
	 * Apply a "like" constraint
	 */
	public AuditCriterion like(T value) {
		return new SimpleAuditExpression( propertyNameGetter, value, " like " );
	}

	/**
	 * Apply a "like" constraint
	 */
	public AuditCriterion like(String value, MatchMode matchMode) {
		return new SimpleAuditExpression( propertyNameGetter, matchMode.toMatchString( value ), " like " );
	}

    /**
     *  Apply an "ilike" constraint
     */
	public AuditCriterion ilike(T value) {
		return new IlikeAuditExpression(propertyNameGetter, value.toString());
	}

    /**
     *  Apply an "ilike" constraint
     */
	public AuditCriterion ilike(String value, MatchMode matchMode) {
		return new IlikeAuditExpression( propertyNameGetter, matchMode.toMatchString( value ));
	}

	/**
	 * Apply a "greater than" constraint
	 */
	public AuditCriterion gt(T value) {
		return new SimpleAuditExpression( propertyNameGetter, value, ">" );
	}

	/**
	 * Apply a "less than" constraint
	 */
	public AuditCriterion lt(T value) {
		return new SimpleAuditExpression( propertyNameGetter, value, "<" );
	}

	/**
	 * Apply a "less than or equal" constraint
	 */
	public AuditCriterion le(T value) {
		return new SimpleAuditExpression( propertyNameGetter, value, "<=" );
	}

	/**
	 * Apply a "greater than or equal" constraint
	 */
	public AuditCriterion ge(T value) {
		return new SimpleAuditExpression( propertyNameGetter, value, ">=" );
	}

	/**
	 * Apply a "between" constraint
	 */
	public AuditCriterion between(T lo, T hi) {
		return new BetweenAuditExpression( propertyNameGetter, lo, hi );
	}

	/**
	 * Apply an "in" constraint
	 */
	public AuditCriterion in(T[] values) {
		return new InAuditExpression( propertyNameGetter, values );
	}

	/**
	 * Apply an "in" constraint
	 */
	public AuditCriterion in(Collection values) {
		return new InAuditExpression( propertyNameGetter, values.toArray() );
	}

	/**
	 * Apply an "is null" constraint
	 */
	public AuditCriterion isNull() {
		return new NullAuditExpression( propertyNameGetter );
	}

	/**
	 * Apply an "equal" constraint to another property
	 */
	public AuditCriterion eqProperty(String otherPropertyName) {
		return new PropertyAuditExpression( propertyNameGetter, otherPropertyName, "=" );
	}

	/**
	 * Apply a "not equal" constraint to another property
	 */
	public AuditCriterion neProperty(String otherPropertyName) {
		return new PropertyAuditExpression( propertyNameGetter, otherPropertyName, "<>" );
	}

	/**
	 * Apply a "less than" constraint to another property
	 */
	public AuditCriterion ltProperty(String otherPropertyName) {
		return new PropertyAuditExpression( propertyNameGetter, otherPropertyName, "<" );
	}

	/**
	 * Apply a "less than or equal" constraint to another property
	 */
	public AuditCriterion leProperty(String otherPropertyName) {
		return new PropertyAuditExpression( propertyNameGetter, otherPropertyName, "<=" );
	}

	/**
	 * Apply a "greater than" constraint to another property
	 */
	public AuditCriterion gtProperty(String otherPropertyName) {
		return new PropertyAuditExpression( propertyNameGetter, otherPropertyName, ">" );
	}

	/**
	 * Apply a "greater than or equal" constraint to another property
	 */
	public AuditCriterion geProperty(String otherPropertyName) {
		return new PropertyAuditExpression( propertyNameGetter, otherPropertyName, ">=" );
	}

	/**
	 * Apply an "is not null" constraint to the another property
	 */
	public AuditCriterion isNotNull() {
		return new NotNullAuditExpression( propertyNameGetter );
	}

	/**
	 * Apply a "maximalize" constraint, with the ability to specify further constraints on the maximized
	 * property
	 */
	public AggregatedAuditExpression maximize() {
		return new AggregatedAuditExpression( propertyNameGetter, AggregatedAuditExpression.AggregatedMode.MAX );
	}

	/**
	 * Apply a "minimize" constraint, with the ability to specify further constraints on the minimized
	 * property
	 */
	public AggregatedAuditExpression minimize() {
		return new AggregatedAuditExpression( propertyNameGetter, AggregatedAuditExpression.AggregatedMode.MIN );
	}

	// Projections

	/**
	 * Projection on the maximum value
	 */
	public AuditProjection max() {
		return new PropertyAuditProjection( propertyNameGetter, "max", false );
	}

	/**
	 * Projection on the minimum value
	 */
	public AuditProjection min() {
		return new PropertyAuditProjection( propertyNameGetter, "min", false );
	}

	/**
	 * Projection counting the values
	 */
	public AuditProjection count() {
		return new PropertyAuditProjection( propertyNameGetter, "count", false );
	}

	/**
	 * Projection counting distinct values
	 */
	public AuditProjection countDistinct() {
		return new PropertyAuditProjection( propertyNameGetter, "count", true );
	}

	/**
	 * Projection on distinct values
	 */
	public AuditProjection distinct() {
		return new PropertyAuditProjection( propertyNameGetter, null, true );
	}

	/**
	 * Projection using a custom function
	 */
	public AuditProjection function(String functionName) {
		return new PropertyAuditProjection( propertyNameGetter, functionName, false );
	}

	// Projection on this property

	public Triple<String, String, Boolean> getData(EnversService enversService) {
		return Triple.make( null, propertyNameGetter.get( enversService ), false );
	}

	// Order

	/**
	 * Sort the results by the property in ascending order
	 */
	public AuditOrder asc() {
		return new PropertyAuditOrder( propertyNameGetter, true );
	}

	/**
	 * Sort the results by the property in descending order
	 */
	public AuditOrder desc() {
		return new PropertyAuditOrder( propertyNameGetter, false );
	}
}
