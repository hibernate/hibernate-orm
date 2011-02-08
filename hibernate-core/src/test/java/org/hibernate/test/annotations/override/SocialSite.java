package org.hibernate.test.annotations.override;
import java.util.Collection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

@Entity
public class SocialSite {

	@Id
	int id;

	String website;

	@ManyToMany(mappedBy="contactInfo.social.website")
	Collection<Employee> employee;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public Collection<Employee> getEmployee() {
		return employee;
	}

	public void setEmployee(Collection<Employee> employee) {
		this.employee = employee;
	}
}
