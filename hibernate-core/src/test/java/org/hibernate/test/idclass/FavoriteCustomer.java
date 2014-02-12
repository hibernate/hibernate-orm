package org.hibernate.test.idclass;

/**
 * @author Emmanuel Bernard
 */
public class FavoriteCustomer extends Customer {
	public FavoriteCustomer() {
	}

	public FavoriteCustomer(String orgName, String custName, String add) {
		super( orgName, custName, add );
	}
}
