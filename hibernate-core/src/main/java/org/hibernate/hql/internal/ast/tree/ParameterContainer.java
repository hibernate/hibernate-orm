/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.hql.internal.ast.tree;
import org.hibernate.param.ParameterSpecification;

/**
 * Currently this is needed in order to deal with {@link FromElement FromElements} which
 * contain "hidden" JDBC parameters from applying filters.
 * <p/>
 * Would love for this to go away, but that would require that Hibernate's
 * internal {@link org.hibernate.engine.internal.JoinSequence join handling} be able to either:<ul>
 * <li>render the same AST structures</li>
 * <li>render structures capable of being converted to these AST structures</li>
 * </ul>
 * <p/>
 * In the interim, this allows us to at least treat these "hidden" parameters properly which is
 * the most pressing need.
 *
 * @deprecated
 * @author Steve Ebersole
 */
public interface ParameterContainer {
	/**
	 * Set the renderable text of this node.
	 *
	 * @param text The renderable text
	 */
	public void setText(String text);

	/**
	 * Adds a parameter specification for a parameter encountered within this node.  We use the term 'embedded' here
	 * because of the fact that the parameter was simply encountered as part of the node's text; it does not exist
	 * as part of a subtree as it might in a true AST.
	 *
	 * @param specification The generated specification.
	 */
	public void addEmbeddedParameter(ParameterSpecification specification);

	/**
	 * Determine whether this node contains embedded parameters.  The implication is that
	 * {@link #getEmbeddedParameters()} is allowed to return null if this method returns false.
	 *
	 * @return True if this node contains embedded parameters; false otherwise.
	 */
	public boolean hasEmbeddedParameters();

	/**
	 * Retrieve all embedded parameter specifications.
	 *
	 * @return All embedded parameter specifications; may return null.
	 * @see #hasEmbeddedParameters()
	 */
	public ParameterSpecification[] getEmbeddedParameters();
}
