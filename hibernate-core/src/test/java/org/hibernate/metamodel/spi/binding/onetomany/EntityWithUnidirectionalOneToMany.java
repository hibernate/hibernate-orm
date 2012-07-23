package org.hibernate.metamodel.spi.binding.onetomany;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.metamodel.spi.binding.SimpleEntity;

/**
 * @author Gail Badner
 */
@Entity
public class EntityWithUnidirectionalOneToMany {
	private Long id;
	private String name;
	private Collection<SimpleEntity> theBag = new ArrayList<SimpleEntity>();
	private Set<SimpleEntity> theSet = new HashSet<SimpleEntity>();
	private List<SimpleEntity> theList = new ArrayList<SimpleEntity>();
	private Map<String, SimpleEntity> theMap = new HashMap<String, SimpleEntity>();
	private Collection<SimpleEntity> thePropertyRefBag = new ArrayList<SimpleEntity>();

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToMany
	public Collection<SimpleEntity> getTheBag() {
		return theBag;
	}

	public void setTheBag(Collection<SimpleEntity> theBag) {
		this.theBag = theBag;
	}

	@OneToMany
	public Set<SimpleEntity> getTheSet() {
		return theSet;
	}

	public void setTheSet(Set<SimpleEntity> theSet) {
		this.theSet = theSet;
	}

	@OneToMany
	public List<SimpleEntity> getTheList() {
		return theList;
	}

	public void setTheList(List<SimpleEntity> theList) {
		this.theList = theList;
	}

	@OneToMany
	public Map<String, SimpleEntity> getTheMap() {
		return theMap;
	}

	public void setTheMap(Map<String, SimpleEntity> theMap) {
		this.theMap = theMap;
	}

	@OneToMany
	public Collection<SimpleEntity> getThePropertyRefSet() {
		return thePropertyRefBag;
	}

	public void setThePropertyRefSet(Set<SimpleEntity> thePropertyRefSet) {
		this.thePropertyRefBag = thePropertyRefSet;
	}
}


