//$Id$
package org.hibernate.test.annotations.immutable;
import java.io.Serializable;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@SuppressWarnings("serial")
public class Country implements Serializable {
	private Integer id;
	
	private String name;
	
	private List<State> states;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setId(Integer integer) {
		id = integer;
	}

	public void setName(String string) {
		name = string;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@Cascade(org.hibernate.annotations.CascadeType.ALL)
	@Immutable
	public List<State> getStates() {
		return states;
	}
	
	public void setStates(List<State> states) {
		this.states = states;
	}
}
