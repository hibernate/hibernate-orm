package org.hibernate.orm.test.annotations.cid;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Product {
	@Id
	public String name;
}
