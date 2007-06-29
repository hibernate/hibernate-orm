package org.hibernate.test.dynamicentity;

/**
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public interface Customer extends Person {
	public Company getCompany();
	public void setCompany(Company company);
}
