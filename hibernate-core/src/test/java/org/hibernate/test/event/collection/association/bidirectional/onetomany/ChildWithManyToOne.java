/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $

package org.hibernate.test.event.collection.association.bidirectional.onetomany;
import org.hibernate.test.event.collection.ChildEntity;
import org.hibernate.test.event.collection.ParentWithCollection;

/**
 * @author Gail Badner
 */
public class ChildWithManyToOne extends ChildEntity {
	private ParentWithCollection parent;

	public ChildWithManyToOne() {
	}

	public ChildWithManyToOne(String name) {
		super( name );
	}

	public ParentWithCollection getParent() {
		return parent;
	}

	public void setParent(ParentWithCollection parent) {
		this.parent = parent;
	}
}
