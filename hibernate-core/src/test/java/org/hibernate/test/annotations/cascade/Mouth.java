//$Id$
package org.hibernate.test.annotations.cascade;
import static javax.persistence.CascadeType.DETACH;
import static javax.persistence.CascadeType.REMOVE;
import java.util.Collection;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

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
	@OneToMany(mappedBy = "mouth", cascade = { REMOVE, DETACH } )
	public Collection<Tooth> teeth;
}
