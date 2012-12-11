//$Id$
package org.hibernate.jpa.test.instrument;

import java.util.Collection;

import org.hibernate.jpa.internal.instrument.InterceptFieldClassFileTransformer;


/**
 * A simple entity to be enhanced by the {@link InterceptFieldClassFileTransformer}
 * 
 * @author Emmanuel Bernard
 * @author Dustin Schultz
 */
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
