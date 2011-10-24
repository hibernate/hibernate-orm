//$Id$
package org.hibernate.test.annotations.id.entities;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Furniture {
	private Integer id;

	@Id
	@GeneratedValue(generator = "hibseq")
	@GenericGenerator(name = "hibseq", strategy = "seqhilo",
			parameters = {
			@Parameter(name = "max_lo", value = "5"),
			@Parameter(name = "sequence", value = "heybabyhey")
					}
	)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
