//$Id$
package org.hibernate.test.annotations.bytecode;


/**
 * @author Emmanuel Bernard
 */
public class Hammer implements Tool {
	private Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer usage() {
		return 0;
	}
}
