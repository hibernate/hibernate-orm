//$Id$
package org.hibernate.jpa.test.ops;


/**
 * @author Gavin King
 */
public class NumberedNode extends Node {

	private long id;

	public NumberedNode() {
		super();
	}


	public NumberedNode(String name) {
		super( name );
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}
