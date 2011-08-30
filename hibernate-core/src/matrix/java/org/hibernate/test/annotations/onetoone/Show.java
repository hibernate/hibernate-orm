//$Id$
package org.hibernate.test.annotations.onetoone;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Show {
	@Id
	private Integer id;
	@OneToOne() private ShowDescription description;


	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public ShowDescription getDescription() {
		return description;
	}

	public void setDescription(ShowDescription description) {
		this.description = description;
	}
}
