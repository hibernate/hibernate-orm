//$Id$
package org.hibernate.test.annotations.manytomany;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Store {
	private Integer id;
	private String name;
	private Set<KnownClient> customers;
	private Set<Supplier> suppliers;

	@ManyToMany(cascade = CascadeType.PERSIST)
	public Set<City> getImplantedIn() {
		return implantedIn;
	}

	public void setImplantedIn(Set<City> implantedIn) {
		this.implantedIn = implantedIn;
	}

	private Set<City> implantedIn;

	@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@JoinTable(
			name = "StoreSupplier",
			joinColumns = @JoinColumn(name = "store"),
			inverseJoinColumns = @JoinColumn(name = "supplier")
	)
	public Set<Supplier> getSuppliers() {
		return suppliers;
	}

	public void setSuppliers(Set<Supplier> suppliers) {
		this.suppliers = suppliers;
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

	@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	public Set<KnownClient> getCustomers() {
		return customers;
	}

	public void setCustomers(Set<KnownClient> customers) {
		this.customers = customers;
	}
}
