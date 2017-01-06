/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.tools.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.internal.tools.MutableBoolean;
import org.hibernate.envers.internal.tools.MutableInteger;

/**
 * Parameters of a query, built using {@link QueryBuilder}.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class Parameters {
	public static final String AND = "and";
	public static final String OR = "or";

	/**
	 * Main alias of the entity.
	 */
	private final String alias;
	/**
	 * Connective between these parameters - "and" or "or".
	 */
	private final String connective;
	/**
	 * For use by the parameter generator. Must be the same in all "child" (and parent) parameters.
	 */
	private final MutableInteger queryParamCounter;

	/**
	 * A list of sub-parameters (parameters with a different connective).
	 */
	private final List<Parameters> subParameters;
	/**
	 * A list of negated parameters.
	 */
	private final List<Parameters> negatedParameters;
	/**
	 * A list of complete where-expressions.
	 */
	private final List<String> expressions;
	/**
	 * Values of parameters used in expressions.
	 */
	private final Map<String, Object> localQueryParamValues;

	Parameters(String alias, String connective, MutableInteger queryParamCounter) {
		this.alias = alias;
		this.connective = connective;
		this.queryParamCounter = queryParamCounter;

		subParameters = new ArrayList<>();
		negatedParameters = new ArrayList<>();
		expressions = new ArrayList<>();
		localQueryParamValues = new HashMap<>();
	}

	// Only for deep copy purpose.
	private Parameters(Parameters other) {
		this.alias = other.alias;
		this.connective = other.connective;
		this.queryParamCounter = other.queryParamCounter.deepCopy();

		subParameters = new ArrayList<>( other.subParameters.size() );
		for ( Parameters p : other.subParameters ) {
			subParameters.add( p.deepCopy() );
		}
		negatedParameters = new ArrayList<>( other.negatedParameters.size() );
		for ( Parameters p : other.negatedParameters ) {
			negatedParameters.add( p.deepCopy() );
		}
		expressions = new ArrayList<>( other.expressions );
		localQueryParamValues = new HashMap<>( other.localQueryParamValues );
	}

	public Parameters deepCopy() {
		return new Parameters( this );
	}

	private String generateQueryParam() {
		return "_p" + queryParamCounter.getAndIncrease();
	}

	/**
	 * Adds sub-parameters with a new connective. That is, the parameters will be grouped in parentheses in the
	 * generated query, e.g.: ... and (exp1 or exp2) and ..., assuming the old connective is "and", and the
	 * new connective is "or".
	 *
	 * @param newConnective New connective of the parameters.
	 *
	 * @return Sub-parameters with the given connective.
	 */
	public Parameters addSubParameters(String newConnective) {
		if ( connective.equals( newConnective ) ) {
			return this;
		}
		else {
			final Parameters newParams = new Parameters( alias, newConnective, queryParamCounter );
			subParameters.add( newParams );
			return newParams;
		}
	}

	/**
	 * Adds negated parameters, by default with the "and" connective. These paremeters will be grouped in parentheses
	 * in the generated query and negated, e.g. ... not (exp1 and exp2) ...
	 *
	 * @return Negated sub paremters.
	 */
	public Parameters addNegatedParameters() {
		final Parameters newParams = new Parameters( alias, AND, queryParamCounter );
		negatedParameters.add( newParams );
		return newParams;
	}

	/**
	 * Adds <code>IS NULL</code> restriction.
	 *
	 * @param propertyName Property name.
	 * @param addAlias Positive if an alias to property name shall be added.
	 */
	public void addNullRestriction(String propertyName, boolean addAlias) {
		addWhere( propertyName, addAlias, "is", "null", false );
	}

	/**
	 * Adds <code>IS NULL</code> restriction.
	 *
	 * @param alias the alias which should be added to the property name.
	 * @param propertyName Property name.
	 */
	public void addNullRestriction(String alias, String propertyName) {
		addWhere( alias, propertyName, "is", null, "null" );
	}

	/**
	 * Adds <code>IS NOT NULL</code> restriction.
	 *
	 * @param propertyName Property name.
	 * @param addAlias Positive if an alias to property name shall be added.
	 */
	public void addNotNullRestriction(String propertyName, boolean addAlias) {
		addWhere( propertyName, addAlias, "is not", "null", false );
	}

	/**
	 * Adds <code>IS NOT NULL</code> restriction.
	 *
	 * @param alias the alias which should be added to the property name.
	 * @param propertyName Property name.
	 */
	public void addNotNullRestriction(String alias, String propertyName) {
		addWhere( alias, propertyName, "is not", null, "null" );
	}

	public void addWhere(String left, boolean addAliasLeft, String op, String right, boolean addAliasRight) {
		addWhere(
				addAliasLeft ? alias : null,
				left,
				op,
				addAliasRight ? alias : null,
				right
		);
	}
	
	public void addWhere(String aliasLeft, String left, String op, String aliasRight, String right) {
		final StringBuilder expression = new StringBuilder();

		if ( aliasLeft != null ) {
			expression.append( aliasLeft ).append( "." );
		}
		expression.append( left );

		expression.append( " " ).append( op ).append( " " );

		if ( aliasRight != null ) {
			expression.append( aliasRight ).append( "." );
		}
		expression.append( right );

		expressions.add( expression.toString() );
	}

	public void addWhereWithFunction(String alias, String left, String leftFunction, String op, Object paramValue){
		final String paramName = generateQueryParam();
		localQueryParamValues.put( paramName, paramValue );
		
		final StringBuilder expression = new StringBuilder();
		
		expression.append( leftFunction ).append( "(" );
		expression.append( alias ).append( "." );
		expression.append( left ).append( ")" );
		expression.append( " " ).append( op ).append( " " );
		expression.append( ":" ).append( paramName );
		
		expressions.add( expression.toString() );
	}

	public void addWhereWithParam(String left, String op, Object paramValue) {
		addWhereWithParam( left, true, op, paramValue );
	}

	public void addWhereWithParam(String alias, String left, String op, Object paramValue ) {
		String effectiveLeft = alias.concat( "." ).concat( left );
		addWhereWithParam( effectiveLeft, false, op, paramValue );
	}

	public void addWhereWithParam(String left, boolean addAlias, String op, Object paramValue) {
		final String paramName = generateQueryParam();
		localQueryParamValues.put( paramName, paramValue );

		addWhereWithNamedParam( left, addAlias, op, paramName );
	}

	public void addWhereWithNamedParam(String left, String op, String paramName) {
		addWhereWithNamedParam( left, true, op, paramName );
	}

	public void addWhereWithNamedParam(String left, boolean addAlias, String op, String paramName) {
		final StringBuilder expression = new StringBuilder();

		if ( addAlias ) {
			expression.append( alias ).append( "." );
		}
		expression.append( left );
		expression.append( " " ).append( op ).append( " " );
		expression.append( ":" ).append( paramName );

		expressions.add( expression.toString() );
	}

	public void addWhereWithParams(String alias, String left, String opStart, Object[] paramValues, String opEnd) {
		final StringBuilder expression = new StringBuilder();

		expression.append( alias ).append( "." ).append( left ).append( " " ).append( opStart );

		for ( int i = 0; i < paramValues.length; i++ ) {
			final Object paramValue = paramValues[i];
			final String paramName = generateQueryParam();
			localQueryParamValues.put( paramName, paramValue );
			expression.append( ":" ).append( paramName );

			if ( i != paramValues.length - 1 ) {
				expression.append( ", " );
			}
		}

		expression.append( opEnd );

		expressions.add( expression.toString() );
	}

	public void addWhere(String left, boolean addAlias, String op, QueryBuilder right) {
		addWhere(
				addAlias ? alias : null,
				left,
				op,
				right
		);
	}

	public void addWhere( String leftAlias, String left, String op, QueryBuilder right) {
		final StringBuilder expression = new StringBuilder();

		if ( leftAlias != null ) {
			expression.append( leftAlias ).append( "." );
		}

		expression.append( left );

		expression.append( " " ).append( op ).append( " " );

		expression.append( "(" );
		right.build( expression, localQueryParamValues );
		expression.append( ")" );

		expressions.add( expression.toString() );
	}

	/**
	 * Add where clause with a null restriction: (left = right or (left is null and right is null))
	 *
	 * @param left Left property name.
	 * @param addAliasLeft Whether to add the alias to the left property.
	 * @param op The operator.
	 * @param right Right property name.
	 * @param addAliasRight Whether to add the alias to the right property.
     */
	public void addWhereOrNullRestriction(String left, boolean addAliasLeft, String op, String right, boolean addAliasRight) {
		// apply the normal addWhere predicate
		final Parameters sub1 = addSubParameters( "or" );
		sub1.addWhere( left, addAliasLeft, op, right, addAliasRight );

		// apply the is null predicate for both join properties
		final Parameters sub2 = sub1.addSubParameters( "and" );
		sub2.addNullRestriction( left, false );
		sub2.addNullRestriction( right, false );
	}

	private void append(StringBuilder sb, String toAppend, MutableBoolean isFirst) {
		if ( !isFirst.isSet() ) {
			sb.append( " " ).append( connective ).append( " " );
		}

		sb.append( toAppend );

		isFirst.unset();
	}

	boolean isEmpty() {
		return expressions.size() == 0 && subParameters.size() == 0 && negatedParameters.size() == 0;
	}

	void build(StringBuilder sb, Map<String, Object> queryParamValues) {
		final MutableBoolean isFirst = new MutableBoolean( true );

		for ( String expression : expressions ) {
			append( sb, expression, isFirst );
		}

		for ( Parameters sub : subParameters ) {
			if ( !subParameters.isEmpty() ) {
				append( sb, "(", isFirst );
				sub.build( sb, queryParamValues );
				sb.append( ")" );
			}
		}

		for ( Parameters negated : negatedParameters ) {
			if ( !negatedParameters.isEmpty() ) {
				append( sb, "not (", isFirst );
				negated.build( sb, queryParamValues );
				sb.append( ")" );
			}
		}

		queryParamValues.putAll( localQueryParamValues );
	}
}
