//$Id$
package org.hibernate.test.annotations.manytomany;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class BuildingCompany extends Company {
	@Column(unique=true)
	@Id @GeneratedValue private Long id;
	private Date foundedIn;

	public Date getFoundedIn() {
		return foundedIn;
	}

	public void setFoundedIn(Date foundedIn) {
		this.foundedIn = foundedIn;
	}

	@Column(unique=true)
	public Long getId() {
		return id;
	}

}
