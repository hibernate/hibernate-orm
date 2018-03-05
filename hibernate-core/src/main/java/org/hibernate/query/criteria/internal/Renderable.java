/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;


import org.hibernate.query.criteria.internal.compile.RenderingContext;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public interface Renderable {

	/**
	 * Render clause
	 *
	 * @param renderingContext context
	 * @return rendered expression
	 */
	String render(RenderingContext renderingContext);

	/**
	 * Render SELECT clause
	 *
	 * @param renderingContext context
	 * @return rendered expression
	 */
	default String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}

	/**
	 * Render GROUP BY clause
	 *
	 * @param renderingContext context
	 *
	 * @return rendered expression
	 */
	default String renderGroupBy(RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
