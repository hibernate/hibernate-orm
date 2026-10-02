package org.hibernate.orm.test.annotations.cid;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Presenter {
	@Id
	public String name;
}
