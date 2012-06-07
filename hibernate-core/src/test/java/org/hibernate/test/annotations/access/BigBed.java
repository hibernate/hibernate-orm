//$Id$
package org.hibernate.test.annotations.access;
import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class BigBed extends Bed {
	@Column(name="bed_size")
	public int size;
}
