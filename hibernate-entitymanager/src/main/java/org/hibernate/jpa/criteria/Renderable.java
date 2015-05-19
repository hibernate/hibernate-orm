/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria;


import org.hibernate.jpa.criteria.compile.RenderingContext;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public interface Renderable {
	public String render(RenderingContext renderingContext);
	public String renderProjection(RenderingContext renderingContext);
}
