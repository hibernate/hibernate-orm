/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class ParameterCollector extends BaseCriteriaVisitor {

	public static Set<ParameterExpression<?>> collectParameters(Criteria criteria) {
		final ParameterCollector collector = new ParameterCollector();
		criteria.accept( collector );
		return collector.parameterExpressions == null
				? Collections.emptySet()
				: collector.parameterExpressions;
	}

	private Set<ParameterExpression<?>> parameterExpressions;

	@Override
	public Object visitParameter(ParameterExpression<?> expression) {
		if ( parameterExpressions == null ) {
			parameterExpressions = new HashSet<>();
		}

		parameterExpressions.add( expression );

		return super.visitParameter( expression );
	}
}
