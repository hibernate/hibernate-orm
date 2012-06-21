//$Id$
package org.hibernate.test.annotations.bytecode;


/**
 * @author Emmanuel Bernard
 */
public interface Tool {
	public Long getId();

	public void setId(Long id);

	public Number usage();
}
