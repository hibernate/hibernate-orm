package org.hibernate.test.dynamicentity;


/**
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public interface Company {
	public Long getId();
	public void setId(Long id);
	public String getName();
	public void setName(String name);
}
