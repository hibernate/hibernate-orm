package org.hibernate.test.annotations.collectionelement;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OrderBy;

import java.util.Set;

@SuppressWarnings({"unchecked", "serial"})

@Entity
public class Products {
	@Id
	@GeneratedValue
	private Integer id;
	
	@ElementCollection
	@OrderBy("name ASC")
	private Set<Widgets> widgets;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<Widgets> getWidgets() {
		return widgets;
	}

	public void setWidgets(Set<Widgets> widgets) {
		this.widgets = widgets;
	}

}