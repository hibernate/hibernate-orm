package org.hibernate.orm.test.bootstrap.binding.annotations.access.jpa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class BigBed extends Bed {
	@Column(name = "bed_size")
	public int size;
}
