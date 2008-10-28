//$Id$
package org.hibernate.test.annotations.fetch;

import java.util.Set;
import java.util.HashSet;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.OneToMany;
import javax.persistence.Entity;
import javax.persistence.FetchType;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Branch {
	@Id
	@GeneratedValue
	private Integer id;

	@OneToMany(mappedBy = "branch", fetch = FetchType.EAGER )
	private Set<Leaf> leaves = new HashSet<Leaf>();


	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<Leaf> getLeaves() {
		return leaves;
	}

	public void setLeaves(Set<Leaf> leaves) {
		this.leaves = leaves;
	}
}
