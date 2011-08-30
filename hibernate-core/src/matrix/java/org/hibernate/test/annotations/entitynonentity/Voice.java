//$Id$
package org.hibernate.test.annotations.entitynonentity;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Voice extends Communication {
	@Id
	@GeneratedValue
	public Integer id;
}
