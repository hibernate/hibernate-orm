/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.criteria;

import java.util.List;
import java.util.Map;

import org.hibernate.criterion.MatchMode;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.EntityInstantiator;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.criteria.internal.FunctionFunctionAuditExpression;
import org.hibernate.envers.query.criteria.internal.PropertyFunctionAuditExpression;
import org.hibernate.envers.query.criteria.internal.SimpleFunctionAuditExpression;
import org.hibernate.envers.query.projection.AuditProjection;

/**
 * Create restrictions or projections using a function.
 * 
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class AuditFunction implements AuditProjection {

	private final String function;
	private final List<Object> arguments;

	public AuditFunction(String function, List<Object> arguments) {
		this.function = function;
		this.arguments = arguments;
	}

	public String getFunction() {
		return function;
	}

	public List<Object> getArguments() {
		return arguments;
	}

	/**
	 * Apply an "equal" constraint
	 */
	public AuditCriterion eq(Object value) {
		return new SimpleFunctionAuditExpression( this, value, "=" );
	}

	/**
	 * Apply a "not equal" constraint
	 */
	public AuditCriterion ne(Object value) {
		return new SimpleFunctionAuditExpression( this, value, "<>" );
	}

	/**
	 * Apply a "like" constraint
	 */
	public AuditCriterion like(Object value) {
		return new SimpleFunctionAuditExpression( this, value, " like " );
	}

	/**
	 * Apply a "like" constraint
	 */
	public AuditCriterion like(String value, MatchMode matchMode) {
		return new SimpleFunctionAuditExpression( this, matchMode.toMatchString( value ), " like " );
	}

	/**
	 * Apply a "greater than" constraint
	 */
	public AuditCriterion gt(Object value) {
		return new SimpleFunctionAuditExpression( this, value, ">" );
	}

	/**
	 * Apply a "less than" constraint
	 */
	public AuditCriterion lt(Object value) {
		return new SimpleFunctionAuditExpression( this, value, "<" );
	}

	/**
	 * Apply a "less than or equal" constraint
	 */
	public AuditCriterion le(Object value) {
		return new SimpleFunctionAuditExpression( this, value, "<=" );
	}

	/**
	 * Apply a "greater than or equal" constraint
	 */
	public AuditCriterion ge(Object value) {
		return new SimpleFunctionAuditExpression( this, value, ">=" );
	}

	/**
	 * Apply an "equal" constraint to a property
	 */
	public AuditCriterion eqProperty(String propertyName) {
		return eqProperty( null, propertyName );
	}

	/**
	 * Apply an "equal" constraint to a property
	 *
	 * @param alias the alias of the entity which owns the property.
	 */
	public AuditCriterion eqProperty(String alias, String propertyName) {
		return new PropertyFunctionAuditExpression( this, alias, propertyName, "=" );
	}

	/**
	 * Apply a "not equal" constraint to a property
	 */
	public AuditCriterion neProperty(String propertyName) {
		return neProperty( null, propertyName );
	}

	/**
	 * Apply a "not equal" constraint to a property
	 *
	 * @param alias the alias of the entity which owns the property.
	 */
	public AuditCriterion neProperty(String alias, String propertyName) {
		return new PropertyFunctionAuditExpression( this, alias, propertyName, "<>" );
	}

	/**
	 * Apply a "less than" constraint to a property
	 */
	public AuditCriterion ltProperty(String propertyName) {
		return ltProperty( null, propertyName );
	}

	/**
	 * Apply a "less than" constraint to a property
	 *
	 * @param alias the alias of the entity which owns the property.
	 */
	public AuditCriterion ltProperty(String alias, String propertyName) {
		return new PropertyFunctionAuditExpression( this, alias, propertyName, "<" );
	}

	/**
	 * Apply a "less than or equal" constraint to a property
	 */
	public AuditCriterion leProperty(String propertyName) {
		return leProperty( null, propertyName );
	}

	/**
	 * Apply a "less than or equal" constraint to a property
	 *
	 * @param alias the alias of the entity which owns the property.
	 */
	public AuditCriterion leProperty(String alias, String propertyName) {
		return new PropertyFunctionAuditExpression( this, alias, propertyName, "<=" );
	}

	/**
	 * Apply a "greater than" constraint to a property
	 */
	public AuditCriterion gtProperty(String propertyName) {
		return gtProperty( null, propertyName );
	}

	/**
	 * Apply a "greater than" constraint to a property
	 *
	 * @param alias the alias of the entity which owns the property.
	 */
	public AuditCriterion gtProperty(String alias, String propertyName) {
		return new PropertyFunctionAuditExpression( this, alias, propertyName, ">" );
	}

	/**
	 * Apply a "greater than or equal" constraint to a property
	 */
	public AuditCriterion geProperty(String propertyName) {
		return geProperty( null, propertyName );
	}

	/**
	 * Apply a "greater than or equal" constraint to a property
	 *
	 * @param alias the alias of the entity which owns the property.
	 */
	public AuditCriterion geProperty(String alias, String propertyName) {
		return new PropertyFunctionAuditExpression( this, alias, propertyName, ">=" );
	}

	/**
	 * Apply an "equal" constraint to another function
	 */
	public AuditCriterion eqFunction(AuditFunction otherFunction) {
		return new FunctionFunctionAuditExpression( this, otherFunction, "=" );
	}

	/**
	 * Apply a "not equal" constraint to another function
	 */
	public AuditCriterion neFunction(AuditFunction otherFunction) {
		return new FunctionFunctionAuditExpression( this, otherFunction, "<>" );
	}

	/**
	 * Apply a "less than" constraint to another function
	 */
	public AuditCriterion ltFunction(AuditFunction otherFunction) {
		return new FunctionFunctionAuditExpression( this, otherFunction, "<" );
	}

	/**
	 * Apply a "less than or equal" constraint to another function
	 */
	public AuditCriterion leFunction(AuditFunction otherFunction) {
		return new FunctionFunctionAuditExpression( this, otherFunction, "<=" );
	}

	/**
	 * Apply a "greater than" constraint to another function
	 */
	public AuditCriterion gtFunction(AuditFunction otherFunction) {
		return new FunctionFunctionAuditExpression( this, otherFunction, ">" );
	}

	/**
	 * Apply a "greater than or equal" constraint to another function
	 */
	public AuditCriterion geFunction(AuditFunction otherFunction) {
		return new FunctionFunctionAuditExpression( this, otherFunction, ">=" );
	}

	@Override
	public void addProjectionToQuery(EnversService enversService, AuditReaderImplementor auditReader,
			Map<String, String> aliasToEntityNameMap, Map<String, String> aliasToComponentPropertyNameMap, String baseAlias, QueryBuilder queryBuilder) {
		queryBuilder.addProjection( enversService, aliasToEntityNameMap, aliasToComponentPropertyNameMap, this );
	}

	@Override
	public Object convertQueryResult(EnversService enversService, EntityInstantiator entityInstantiator,
			String entityName, Number revision, Object value) {
		return value;
	}

	@Override
	public String getAlias(String baseAlias) {
		return baseAlias;
	}

}
