/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hbm.index;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.tool.schema.internal.exec.GenerationTarget;

/**
 * @author Steve Ebersole
 */
public class JournalingSchemaToolingTarget implements GenerationTarget {
	private List<String> actions = new ArrayList<String>();

	public JournalingSchemaToolingTarget() {
	}

	@Override
	public void prepare() {

	}

	@Override
	public void accept(String action) {
		actions.add( action );
	}

	@Override
	public void release() {

	}

	public List<String> getActions() {
		return actions;
	}

	public boolean containedText(String text) {
		for ( String action : actions ) {
			if ( action.contains( text ) ) {
				return true;
			}
		}

		return false;
	}
}
