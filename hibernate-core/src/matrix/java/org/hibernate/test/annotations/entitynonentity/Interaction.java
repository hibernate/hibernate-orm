//$Id$
package org.hibernate.test.annotations.entitynonentity;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public class Interaction {
	@Column(name="int_nbr")
	public int number;
}
