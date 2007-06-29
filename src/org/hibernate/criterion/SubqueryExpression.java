//$Id$
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.TypedValue;
import org.hibernate.impl.CriteriaImpl;
import org.hibernate.loader.criteria.CriteriaQueryTranslator;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.sql.Select;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
public abstract class SubqueryExpression implements Criterion {
	
	private CriteriaImpl criteriaImpl;
	private String quantifier;
	private String op;
	private QueryParameters params;
	private Type[] types;
	
	protected Type[] getTypes() {
		return types;
	}
	
	protected SubqueryExpression(String op, String quantifier, DetachedCriteria dc) {
		this.criteriaImpl = dc.getCriteriaImpl();
		this.quantifier = quantifier;
		this.op = op;
	}
	
	protected abstract String toLeftSqlString(Criteria criteria, CriteriaQuery outerQuery);

	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		
		final SessionImplementor session = ( (CriteriaImpl) criteria ).getSession(); //ugly!
		final SessionFactoryImplementor factory = session.getFactory();
		
		final OuterJoinLoadable persister = (OuterJoinLoadable) factory.getEntityPersister( criteriaImpl.getEntityOrClassName() );
		CriteriaQueryTranslator innerQuery = new CriteriaQueryTranslator( 
				factory, 
				criteriaImpl, 
				criteriaImpl.getEntityOrClassName(), //implicit polymorphism not supported (would need a union) 
				criteriaQuery.generateSQLAlias(),
				criteriaQuery
			);
		
		params = innerQuery.getQueryParameters(); //TODO: bad lifecycle....
		types = innerQuery.getProjectedTypes();
		
		//String filter = persister.filterFragment( innerQuery.getRootSQLALias(), session.getEnabledFilters() );
		
		String sql = new Select( factory.getDialect() )
			.setWhereClause( innerQuery.getWhereCondition() )
			.setGroupByClause( innerQuery.getGroupBy() )
			.setSelectClause( innerQuery.getSelect() )
			.setFromClause(
					persister.fromTableFragment( innerQuery.getRootSQLALias() ) +   
					persister.fromJoinFragment( innerQuery.getRootSQLALias(), true, false )
				)
			.toStatementString();
		
		final StringBuffer buf = new StringBuffer()
			.append( toLeftSqlString(criteria, criteriaQuery) );
		if (op!=null) buf.append(' ').append(op).append(' ');
		if (quantifier!=null) buf.append(quantifier).append(' ');
		return buf.append('(').append(sql).append(')')
			.toString();
	}

	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		Type[] types = params.getPositionalParameterTypes();
		Object[] values = params.getPositionalParameterValues();
		TypedValue[] tv = new TypedValue[types.length];
		for ( int i=0; i<types.length; i++ ) {
			tv[i] = new TypedValue( types[i], values[i], EntityMode.POJO );
		}
		return tv;
	}

}
