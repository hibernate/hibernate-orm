//$Id$
package org.hibernate.test.annotations.join;
import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SecondaryTable;

/**
 * @author Emmanuel Bernard
 */
@Entity
@SecondaryTable(name = "ExtendedLife")
public class Life implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "LIFE_ID")
	public Integer id;

	public int duration;
	@Column(table = "ExtendedLife")
	public String fullDescription;

	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@JoinColumn(name = "CAT_ID", table = "ExtendedLife")
	public Cat owner;

}
