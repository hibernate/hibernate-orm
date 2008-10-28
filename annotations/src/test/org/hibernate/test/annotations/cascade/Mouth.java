//$Id$
package org.hibernate.test.annotations.cascade;

import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Column;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Mouth {
	@Id
	@GeneratedValue
	public Integer id;
	@Column(name="mouth_size")
	public int size;
	@OneToMany(mappedBy = "mouth", cascade = CascadeType.REMOVE)
	public Collection<Tooth> teeth;
}
