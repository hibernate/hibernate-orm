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
	private List embeddedParameters;

	public void addEmbeddedParameter(ParameterSpecification specification) {
		if ( embeddedParameters == null ) {
			embeddedParameters = new ArrayList();
		}
		embeddedParameters.add( specification );
	}

	public boolean hasEmbeddedParameters() {
		return embeddedParameters != null && ! embeddedParameters.isEmpty();
	}

	public ParameterSpecification[] getEmbeddedParameters() {
		return ( ParameterSpecification[] ) embeddedParameters.toArray( new ParameterSpecification[ embeddedParameters.size() ] );
	}
}
