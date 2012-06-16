package org.hibernate.test.annotations.cid;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Product {
	@Id
    public String name;
}
