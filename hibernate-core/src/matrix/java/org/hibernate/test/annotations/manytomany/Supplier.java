//$Id$
package org.hibernate.test.annotations.manytomany;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Supplier {
	private Integer id;
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private Set<Store> suppStores;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ManyToMany(mappedBy = "suppliers")
	public Set<Store> getSuppStores() {
		return suppStores;
	}

	public void setSuppStores(Set<Store> suppStores) {
		this.suppStores = suppStores;
	}
}
