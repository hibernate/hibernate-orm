package org.hibernate.test.annotations.override;

import javax.persistence.Embedded;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class Address {
	public String street;
	public String city; 
	public String state;
}
