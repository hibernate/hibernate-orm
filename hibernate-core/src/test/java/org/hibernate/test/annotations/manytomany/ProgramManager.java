package org.hibernate.test.annotations.manytomany;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
public class ProgramManager {
	int id;

	Collection<Employee> manages;

	@Id
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@OneToMany( mappedBy="jobInfo.pm", cascade= CascadeType.ALL )
	public Collection<Employee> getManages() {
		return manages;
	}

	public void setManages( Collection<Employee> manages ) {
		this.manages = manages;
	}

}
