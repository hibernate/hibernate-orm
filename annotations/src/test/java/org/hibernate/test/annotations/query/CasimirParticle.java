//$Id$
package org.hibernate.test.annotations.query;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name="CASIMIR_PARTICULE")
public class CasimirParticle {
	@Id
	private Long id;


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
