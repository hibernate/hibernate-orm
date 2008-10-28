//$Id:PrimeMinister.java 9793 2006-04-26 02:20:18 -0400 (mer., 26 avr. 2006) epbernard $
package org.hibernate.test.annotations.xml.hbm;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class PrimeMinister {
	private Integer id;
	private String name;
	private Government currentGovernment;
	private Set<Government> governments;

	@ManyToOne
	public Government getCurrentGovernment() {
		return currentGovernment;
	}

	public void setCurrentGovernment(Government currentGovernment) {
		this.currentGovernment = currentGovernment;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToMany(mappedBy = "primeMinister")
	public Set<Government> getGovernments() {
		return governments;
	}

	public void setGovernments(Set<Government> governments) {
		this.governments = governments;
	}

}
