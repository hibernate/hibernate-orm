/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.param.ParameterSpecification;
import org.hibernate.sql.JoinFragment;

/**
 * Represents an SQL fragment in the AST.
 *
 * @author josh
 */
public class SqlFragment extends Node implements ParameterContainer {
	private JoinFragment joinFragment;
	private FromElement fromElement;

	public void setJoinFragment(JoinFragment joinFragment) {
		this.joinFragment = joinFragment;
	}

	public boolean hasFilterCondition() {
		return joinFragment.hasFilterCondition();
	}

	public void setFromElement(FromElement fromElement) {
		this.fromElement = fromElement;
	}

	public FromElement getFromElement() {
		return fromElement;
	}


	// ParameterContainer impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private List<ParameterSpecification> embeddedParameters;

	public void addEmbeddedParameter(ParameterSpecification specification) {
		if ( embeddedParameters == null ) {
			embeddedParameters = new ArrayList<ParameterSpecification>();
		}
		embeddedParameters.add( specification );
	}

	public boolean hasEmbeddedParameters() {
		return embeddedParameters != null && ! embeddedParameters.isEmpty();
	}

	public ParameterSpecification[] getEmbeddedParameters() {
		return embeddedParameters.toArray( new ParameterSpecification[ embeddedParameters.size() ] );
	}
}
