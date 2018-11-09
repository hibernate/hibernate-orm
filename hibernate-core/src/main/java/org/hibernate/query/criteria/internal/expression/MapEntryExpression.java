/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CompoundSelection;
import javax.persistence.criteria.Selection;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.path.MapAttributeJoin;
import org.hibernate.sql.ast.Clause;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class MapEntryExpression<K,V>
		extends ExpressionImpl<Map.Entry<K,V>>
		implements CompoundSelection<Map.Entry<K,V>>, Serializable {

	private final MapAttributeJoin<?, K, V> original;

	public MapEntryExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<Map.Entry<K, V>> javaType,
			MapAttributeJoin<?, K, V> original) {
		super( criteriaBuilder, javaType );
		this.original = original;
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		// none to register
	}

	@Override
	public String render(RenderingContext renderingContext) {
		if ( renderingContext.getClauseStack().getCurrent() == Clause.SELECT ) {
			return "entry(" + original.render( renderingContext ) + ")";
		}

		// don't think this is valid outside of select clause...
		throw new IllegalStateException( "illegal reference to map entry outside of select clause." );
	}

	@Override
	public boolean isCompoundSelection() {
		return true;
	}

	@Override
	public List<Selection<?>> getCompoundSelectionItems() {
		return Arrays.asList( original.key(), original.value() );
	}

}
