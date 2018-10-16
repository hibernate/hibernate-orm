/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;


import org.hibernate.query.criteria.internal.compile.RenderingContext;

/**
 * Contract for nodes in the JPA Criteria tree that can be rendered
 * as part of criteria "compilation"
 *
 * @author Steve Ebersole
 */
public interface Renderable {

	/**
	 * Perform the rendering, returning the rendition
	 */
	String render(RenderingContext renderingContext);
}
