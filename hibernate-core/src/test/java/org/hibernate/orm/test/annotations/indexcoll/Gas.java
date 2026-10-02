package org.hibernate.orm.test.annotations.indexcoll;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Gas {
	@Id
	@GeneratedValue
	public Integer id;
	public String name;

}
