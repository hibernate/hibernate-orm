/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.criteria;

import java.util.Collection;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.EntityInstantiator;
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
	private final String alias;
	private final PropertyNameGetter propertyNameGetter;

	public AuditProperty(String alias, PropertyNameGetter propertyNameGetter) {
		this.alias = alias;
		this.propertyNameGetter = propertyNameGetter;
	}

	public AuditCriterion hasChanged() {
		return new SimpleAuditExpression( alias, new ModifiedFlagPropertyName( propertyNameGetter ), true, "=" );
	}

	public AuditCriterion hasNotChanged() {
		return new SimpleAuditExpression( alias, new ModifiedFlagPropertyName( propertyNameGetter ), false, "=" );
	}

	/**
	 * Apply an "equal" constraint
	 */
	public AuditCriterion eq(T value) {
		return new SimpleAuditExpression( alias, propertyNameGetter, value, "=" );
	}

	/**
	 * Apply a "not equal" constraint
	 */
	public AuditCriterion ne(T value) {
		return new SimpleAuditExpression( alias, propertyNameGetter, value, "<>" );
	}

	/**
	 * Apply a "like" constraint
	 */
	public AuditCriterion like(T value) {
		return new SimpleAuditExpression( alias, propertyNameGetter, value, " like " );
	}

	/**
	 * Apply a "like" constraint
	 *
	 * @deprecated since 5.2, use {@link #like(String, MatchMode)}.
	 */
	@Deprecated
	public AuditCriterion like(String value, org.hibernate.criterion.MatchMode matchMode) {
		return new SimpleAuditExpression( alias, propertyNameGetter, matchMode.toMatchString( value ), " like" );
	}

	/**
	 * Apply a "like" constraint
	 */
	public AuditCriterion like(String value, MatchMode matchMode) {
		return new SimpleAuditExpression( alias, propertyNameGetter, matchMode.toMatchString( value ), " like " );
	}

    /**
     *  Apply an "ilike" constraint
     */
	public AuditCriterion ilike(T value) {
		return new IlikeAuditExpression( alias, propertyNameGetter, value.toString() );
	}

	/**
	 * Apply an "ilike" constraint
	 *
	 * @deprecated since 5.2, use {@link #ilike(String, MatchMode)}.
	 */
	@Deprecated
	public AuditCriterion ilike(String value, org.hibernate.criterion.MatchMode matchMode) {
		return new IlikeAuditExpression( alias, propertyNameGetter, matchMode.toMatchString( value ) );
	}

	/**
	 * Apply on "ilike" constraint
	 */
	public AuditCriterion ilike(String value, MatchMode matchMode) {
		return new IlikeAuditExpression( alias, propertyNameGetter, matchMode.toMatchString( value ) );
	}

	/**
	 * Apply a "greater than" constraint
	 */
	public AuditCriterion gt(T value) {
		return new SimpleAuditExpression( alias, propertyNameGetter, value, ">" );
	}

	/**
	 * Apply a "less than" constraint
	 */
	public AuditCriterion lt(T value) {
		return new SimpleAuditExpression( alias, propertyNameGetter, value, "<" );
	}

	/**
	 * Apply a "less than or equal" constraint
	 */
	public AuditCriterion le(T value) {
		return new SimpleAuditExpression( alias, propertyNameGetter, value, "<=" );
	}

	/**
	 * Apply a "greater than or equal" constraint
	 */
	public AuditCriterion ge(T value) {
		return new SimpleAuditExpression( alias, propertyNameGetter, value, ">=" );
	}

	/**
	 * Apply a "between" constraint
	 */
	public AuditCriterion between(T lo, T hi) {
		return new BetweenAuditExpression( alias, propertyNameGetter, lo, hi );
	}

	/**
	 * Apply an "in" constraint
	 */
	public AuditCriterion in(T[] values) {
		return new InAuditExpression( alias, propertyNameGetter, values );
	}

	/**
	 * Apply an "in" constraint
	 */
	public AuditCriterion in(Collection values) {
		return new InAuditExpression( alias, propertyNameGetter, values.toArray() );
	}

	/**
	 * Apply an "is null" constraint
	 */
	public AuditCriterion isNull() {
		return new NullAuditExpression( alias, propertyNameGetter );
	}

	/**
	 * Apply an "equal" constraint to another property
	 */
	public AuditCriterion eqProperty(String otherPropertyName) {
		/*
		 * We provide alias as otherAlias rather than null, because this seems the intuitive use case.
		 * E.g. if the user calls AuditEntity.property( "alias", "prop" ).eqProperty( "otherProp" )
		 * it is assumed that the otherProp is on the same entity as prop and therefore we have to use
		 * the same alias.
		 */
		return eqProperty( alias, otherPropertyName );
	}

	/**
	 * Apply an "equal" constraint to another property
	 *
	 * @param otherAlias the alias of the entity which owns the other property.
	 */
	public AuditCriterion eqProperty(String otherAlias, String otherPropertyName) {
		return new PropertyAuditExpression( alias, propertyNameGetter, otherAlias, otherPropertyName, "=" );
	}

	/**
	 * Apply a "not equal" constraint to another property
	 */
	public AuditCriterion neProperty(String otherPropertyName) {
		/*
		 * We provide alias as otherAlias rather than null, because this seems the intuitive use case.
		 * E.g. if the user calls AuditEntity.property( "alias", "prop" ).neProperty( "otherProp" )
		 * it is assumed that the otherProp is on the same entity as prop and therefore we have to use
		 * the same alias.
		 */
		return neProperty( alias, otherPropertyName );
	}

	/**
	 * Apply a "not equal" constraint to another property
	 *
	 * @param otherAlias the alias of the entity which owns the other property.
	 */
	public AuditCriterion neProperty(String otherAlias, String otherPropertyName) {
		return new PropertyAuditExpression( alias, propertyNameGetter, otherAlias, otherPropertyName, "<>" );
	}

	/**
	 * Apply a "less than" constraint to another property
	 */
	public AuditCriterion ltProperty(String otherPropertyName) {
		/*
		 * We provide alias as otherAlias rather than null, because this seems the intuitive use case.
		 * E.g. if the user calls AuditEntity.property( "alias", "prop" ).ltProperty( "otherProp" )
		 * it is assumed that the otherProp is on the same entity as prop and therefore we have to use
		 * the same alias.
		 */
		return ltProperty( alias, otherPropertyName );
	}

	/**
	 * Apply a "less than" constraint to another property
	 *
	 * @param otherAlias the alias of the entity which owns the other property.
	 */
	public AuditCriterion ltProperty(String otherAlias, String otherPropertyName) {
		return new PropertyAuditExpression( alias, propertyNameGetter, otherAlias, otherPropertyName, "<" );
	}

	/**
	 * Apply a "less than or equal" constraint to another property
	 */
	public AuditCriterion leProperty(String otherPropertyName) {
		/*
		 * We provide alias as otherAlias rather than null, because this seems the intuitive use case.
		 * E.g. if the user calls AuditEntity.property( "alias", "prop" ).leProperty( "otherProp" )
		 * it is assumed that the otherProp is on the same entity as prop and therefore we have to use
		 * the same alias.
		 */
		return leProperty( alias, otherPropertyName );
	}

	/**
	 * Apply a "less than or equal" constraint to another property
	 *
	 * @param otherAlias the alias of the entity which owns the other property.
	 */
	public AuditCriterion leProperty(String otherAlias, String otherPropertyName) {
		return new PropertyAuditExpression( alias, propertyNameGetter, otherAlias, otherPropertyName, "<=" );
	}

	/**
	 * Apply a "greater than" constraint to another property
	 */
	public AuditCriterion gtProperty(String otherPropertyName) {
		/*
		 * We provide alias as otherAlias rather than null, because this seems the intuitive use case.
		 * E.g. if the user calls AuditEntity.property( "alias", "prop" ).gtProperty( "otherProp" )
		 * it is assumed that the otherProp is on the same entity as prop and therefore we have to use
		 * the same alias.
		 */
		return gtProperty( alias, otherPropertyName );
	}

	/**
	 * Apply a "greater than" constraint to another property
	 *
	 * @param otherAlias the alias of the entity which owns the other property.
	 */
	public AuditCriterion gtProperty(String otherAlias, String otherPropertyName) {
		return new PropertyAuditExpression( alias, propertyNameGetter, otherAlias, otherPropertyName, ">" );
	}

	/**
	 * Apply a "greater than or equal" constraint to another property
	 */
	public AuditCriterion geProperty(String otherPropertyName) {
		/*
		 * We provide alias as otherAlias rather than null, because this seems the intuitive use case.
		 * E.g. if the user calls AuditEntity.property( "alias", "prop" ).geProperty( "otherProp" )
		 * it is assumed that the otherProp is on the same entity as prop and therefore we have to use
		 * the same alias.
		 */
		return geProperty( alias, otherPropertyName );
	}

	/**
	 * Apply a "greater than or equal" constraint to another property
	 *
	 * @param otherAlias the alias of the entity which owns the other property.
	 */
	public AuditCriterion geProperty(String otherAlias, String otherPropertyName) {
		return new PropertyAuditExpression( alias, propertyNameGetter, otherAlias, otherPropertyName, ">=" );
	}

	/**
	 * Apply an "is not null" constraint to the another property
	 */
	public AuditCriterion isNotNull() {
		return new NotNullAuditExpression( alias, propertyNameGetter );
	}

	/**
	 * Apply a "maximalize" constraint, with the ability to specify further constraints on the maximized
	 * property
	 */
	public AggregatedAuditExpression maximize() {
		return new AggregatedAuditExpression( alias, propertyNameGetter, AggregatedAuditExpression.AggregatedMode.MAX );
	}

	/**
	 * Apply a "minimize" constraint, with the ability to specify further constraints on the minimized
	 * property
	 */
	public AggregatedAuditExpression minimize() {
		return new AggregatedAuditExpression( alias, propertyNameGetter, AggregatedAuditExpression.AggregatedMode.MIN );
	}

	// Projections

	/**
	 * Projection on the maximum value
	 */
	public AuditProjection max() {
		return new PropertyAuditProjection( alias, propertyNameGetter, "max", false );
	}

	/**
	 * Projection on the minimum value
	 */
	public AuditProjection min() {
		return new PropertyAuditProjection( alias, propertyNameGetter, "min", false );
	}

	/**
	 * Projection counting the values
	 */
	public AuditProjection count() {
		return new PropertyAuditProjection( alias, propertyNameGetter, "count", false );
	}

	/**
	 * Projection counting distinct values
	 */
	public AuditProjection countDistinct() {
		return new PropertyAuditProjection( alias, propertyNameGetter, "count", true );
	}

	/**
	 * Projection on distinct values
	 */
	public AuditProjection distinct() {
		return new PropertyAuditProjection( alias, propertyNameGetter, null, true );
	}

	/**
	 * Projection using a custom function
	 */
	public AuditProjection function(String functionName) {
		return new PropertyAuditProjection( alias, propertyNameGetter, functionName, false );
	}

	// Projection on this property

	public ProjectionData getData(EnversService enversService) {
		return new ProjectionData( null, alias, propertyNameGetter.get( enversService ), false );
	}

	// Order

	/**
	 * Sort the results by the property in ascending order
	 */
	public AuditOrder asc() {
		return new PropertyAuditOrder( alias, propertyNameGetter, true );
	}

	/**
	 * Sort the results by the property in descending order
	 */
	public AuditOrder desc() {
		return new PropertyAuditOrder( alias, propertyNameGetter, false );
	}
	
	@Override
	public Object convertQueryResult(EnversService enversService, EntityInstantiator entityInstantiator, String entityName, Number revision, Object value) {
		return value;
	}
	
}
