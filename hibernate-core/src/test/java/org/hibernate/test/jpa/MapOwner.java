package org.hibernate.test.jpa;

import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;

@Entity
public class MapOwner {

	@Id
	private Long id;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@MapKey(name = "relationship")
	private Map<Relationship, MapContent> contents;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Map<Relationship, MapContent> getContents() {
		return contents;
	}
	public void setContents(Map<Relationship, MapContent> contents) {
		this.contents = contents;
	}

}
