/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression;

import java.io.Serializable;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.path.PluralAttributePath;

/**
 * Represents a "size of" expression in regards to a persistent collection; the implication is
 * that of a subquery.
 *
 * @author Steve Ebersole
 */
public class SizeOfPluralAttributeExpression
		extends ExpressionImpl<Integer>
		implements Serializable {
	private final PluralAttributePath path;

	public SizeOfPluralAttributeExpression(
			CriteriaBuilderImpl criteriaBuilder,
			PluralAttributePath path) {
		super( criteriaBuilder, Integer.class);
		this.path = path;
	}

	/**
	 * @deprecated Use {@link #getPluralAttributePath()} instead
	 */
	@Deprecated
	public PluralAttributePath getCollectionPath() {
		return path;
	}

	public PluralAttributePath getPluralAttributePath() {
		return path;
	}

	public void registerParameters(ParameterRegistry registry) {
		// nothing to do
	}

	public String render(RenderingContext renderingContext) {
		return "size(" + getPluralAttributePath().render( renderingContext ) + ")";
	}

	public String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
