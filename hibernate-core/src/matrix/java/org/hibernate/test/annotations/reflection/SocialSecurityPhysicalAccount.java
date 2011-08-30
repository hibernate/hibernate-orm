//$Id$
package org.hibernate.test.annotations.reflection;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class SocialSecurityPhysicalAccount {
	public String number;
	public String countryCode;
}
