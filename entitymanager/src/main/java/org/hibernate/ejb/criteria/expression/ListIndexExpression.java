package org.hibernate.ejb.criteria.expression;

import javax.persistence.metamodel.ListAttribute;
import org.hibernate.ejb.criteria.QueryBuilderImpl;

/**
 * An expression for referring to the index of a list.
 *
 * @author Steve Ebersole
 */
public class ListIndexExpression extends ExpressionImpl<Integer> {
	private final ListAttribute<?,?> listAttribute;

	public ListIndexExpression(QueryBuilderImpl queryBuilder, ListAttribute<?,?> listAttribute) {
		super( queryBuilder, Integer.class );
		this.listAttribute = listAttribute;
	}

	public ListAttribute<?,?> getListAttribute() {
		return listAttribute;
	}

}
