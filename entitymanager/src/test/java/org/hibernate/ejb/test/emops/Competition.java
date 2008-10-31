//$Id: $
package org.hibernate.ejb.test.emops;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.ManyToMany;
import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.JoinTable;
import javax.persistence.JoinColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Competition {
	@Id
	@GeneratedValue
	private Integer id;

	@ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE },
			fetch = FetchType.LAZY)
	@JoinTable(name="competition_competitor")
	@JoinColumn
	private List<Competitor> competitors = new ArrayList<Competitor>();


	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public List<Competitor> getCompetitors() {
		return competitors;
	}

	public void setCompetitors(List<Competitor> competitors) {
		this.competitors = competitors;
	}
}
