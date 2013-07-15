package org.hibernate.test.annotations.collectionelement;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OrderBy;

@SuppressWarnings({"unchecked", "serial"})

@Entity
public class BugSystem {
	@Id
	@GeneratedValue
	private Integer id;

	@ElementCollection
	@OrderBy("reportedBy.lastName ASC,reportedBy.firstName ASC,summary")
	private Set<Bug> bugs;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<Bug> getBugs() {
		return bugs;
	}

	public void setBugs(Set<Bug> bugs) {
		this.bugs = bugs;
	}

}
