/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.jpa.test.instrument;

import java.util.Collection;

import javax.persistence.Entity;

import org.hibernate.jpa.internal.instrument.InterceptFieldClassFileTransformer;


/**
 * A simple entity to be enhanced by the {@link InterceptFieldClassFileTransformer}
 * 
 * @author Emmanuel Bernard
 * @author Dustin Schultz
 */
@Entity
public class Simple {
	private String name;
	
	// Have an additional attribute that will ensure that the enhanced classes
	// will see all class attributes of an entity without CNFEs
	private Collection<SimpleRelation> relations;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Collection<SimpleRelation> getRelations() {
		return relations;
	}

	public void setRelations(Collection<SimpleRelation> relations) {
		this.relations = relations;
	}
}
