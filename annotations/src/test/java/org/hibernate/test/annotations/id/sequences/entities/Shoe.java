//$Id: Shoe.java 14760 2008-06-11 07:33:15Z hardy.ferentschik $
package org.hibernate.test.annotations.id.sequences.entities;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * sample of Sequance generator
 *
 * @author Emmanuel Bernard
 */
@Entity
@SuppressWarnings("serial")
public class Shoe implements Serializable {
	private Long id;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GEN")
	public Long getId() {
		return id;
	}

	public void setId(Long long1) {
		id = long1;
	}
}
