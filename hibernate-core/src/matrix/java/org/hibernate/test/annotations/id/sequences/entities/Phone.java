//$Id: Phone.java 14760 2008-06-11 07:33:15Z hardy.ferentschik $
package org.hibernate.test.annotations.id.sequences.entities;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity()
public class Phone {
	private Integer id;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Phone_Gen")
	@javax.persistence.SequenceGenerator(
			name = "Phone_Gen",
			sequenceName = "phone_seq"
	)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
