//$Id$
package org.hibernate.test.annotations.reflection;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.IdClass;
import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;

/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(SocialSecurityNumber.class)
@DiscriminatorValue("Moral")
@SequenceGenerator(name = "seq")
@TableGenerator(name = "table")
public class SocialSecurityMoralAccount {
	public String number;
	public String countryCode;
}
