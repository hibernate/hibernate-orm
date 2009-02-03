// $Id$
package org.hibernate.hql.ast.tree;

import org.hibernate.param.ParameterSpecification;
import org.hibernate.type.Type;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.hql.ast.util.DisplayableNode;

/**
 * Implementation of ParameterNode.
 *
 * @author Steve Ebersole
 */
public class ParameterNode extends HqlSqlWalkerNode implements DisplayableNode, ExpectedTypeAwareNode {
	private ParameterSpecification parameterSpecification;

	public ParameterSpecification getHqlParameterSpecification() {
		return parameterSpecification;
	}

	public void setHqlParameterSpecification(ParameterSpecification parameterSpecification) {
		this.parameterSpecification = parameterSpecification;
	}

	public String getDisplayText() {
		return "{" + ( parameterSpecification == null ? "???" : parameterSpecification.renderDisplayInfo() ) + "}";
	}

	public void setExpectedType(Type expectedType) {
		getHqlParameterSpecification().setExpectedType( expectedType );
		setDataType( expectedType );
	}

	public Type getExpectedType() {
		return getHqlParameterSpecification() == null ? null : getHqlParameterSpecification().getExpectedType();
	}

	public String getRenderText(SessionFactoryImplementor sessionFactory) {
		int count = 0;
		if ( getExpectedType() != null && ( count = getExpectedType().getColumnSpan( sessionFactory ) ) > 1 ) {
			StringBuffer buffer = new StringBuffer();
			buffer.append( "(?" );
			for ( int i = 1; i < count; i++ ) {
				buffer.append( ", ?" );
			}
			buffer.append( ")" );
			return buffer.toString();
		}
		else {
			return "?";
		}
	}
}
