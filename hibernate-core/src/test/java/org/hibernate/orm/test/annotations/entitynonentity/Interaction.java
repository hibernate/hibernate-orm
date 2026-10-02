package org.hibernate.orm.test.annotations.entitynonentity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public class Interaction {
	@Column(name="int_nbr")
	public int number;
}
