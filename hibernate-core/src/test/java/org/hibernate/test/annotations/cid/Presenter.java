//$Id$
package org.hibernate.test.annotations.cid;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Presenter {
	@Id
	public String name;
}
