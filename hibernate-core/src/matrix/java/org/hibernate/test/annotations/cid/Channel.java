//$Id$
package org.hibernate.test.annotations.cid;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Channel {
	@Id
	@GeneratedValue
	public Integer id;
}
