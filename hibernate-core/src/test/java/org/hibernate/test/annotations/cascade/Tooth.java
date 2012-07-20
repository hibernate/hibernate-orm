//$Id$
package org.hibernate.test.annotations.cascade;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Column;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Tooth {
	@Id
	@GeneratedValue
	public Integer id;
    @Column(name = "`type`")
	public String type;
	@ManyToOne(cascade = CascadeType.PERSIST)
	public Tooth leftNeighbour;
	@ManyToOne(cascade = CascadeType.MERGE)
	public Tooth rightNeighbour;
	@ManyToOne
	public Mouth mouth;
}
