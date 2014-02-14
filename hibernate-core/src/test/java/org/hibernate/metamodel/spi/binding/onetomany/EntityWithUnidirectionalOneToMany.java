package org.hibernate.metamodel.spi.binding.onetomany;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
/**
 * @author Gail Badner
 */
@Entity
public class EntityWithUnidirectionalOneToMany {
	private Long id;
	private String name;

	private Collection<ReferencedEntity> theBag = new ArrayList<ReferencedEntity>();
	private Set<ReferencedEntity> theSet = new HashSet<ReferencedEntity>();
	private List<ReferencedEntity> theList = new ArrayList<ReferencedEntity>();
	private Map<String, ReferencedEntity> theMap = new HashMap<String, ReferencedEntity>();
	private Collection<ReferencedEntity> thePropertyRefBag = new ArrayList<ReferencedEntity>();


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
	@JoinColumn
	public Collection<ReferencedEntity> getTheBag() {
		return theBag;
	}

	public void setTheBag(Collection<ReferencedEntity> theBag) {
		this.theBag = theBag;
	}

	@OneToMany(fetch = FetchType.EAGER)
	@JoinColumn(name = "theSetOwner", nullable = false)
	public Set<ReferencedEntity> getTheSet() {
		return theSet;
	}

	public void setTheSet(Set<ReferencedEntity> theSet) {
		this.theSet = theSet;
	}

	@OneToMany(fetch = FetchType.EAGER)
	@JoinColumn(name = "theListOwner", nullable = false)
	@OrderColumn( name = "list_index")
	public List<ReferencedEntity> getTheList() {
		return theList;
	}

	public void setTheList(List<ReferencedEntity> theList) {
		this.theList = theList;
	}

	@OneToMany
	@JoinColumn(name = "theMapOwner", nullable = false)
	public Map<String, ReferencedEntity> getTheMap() {
		return theMap;
	}

	public void setTheMap(Map<String, ReferencedEntity> theMap) {
		this.theMap = theMap;
	}

	@OneToMany
	@JoinColumn(name = "ownerName", nullable = false, referencedColumnName = "name")
	@LazyCollection( LazyCollectionOption.EXTRA )
	public Collection<ReferencedEntity> getThePropertyRefSet() {
		return thePropertyRefBag;
	}

	public void setThePropertyRefSet(Set<ReferencedEntity> thePropertyRefSet) {
		this.thePropertyRefBag = thePropertyRefSet;
	}

}


