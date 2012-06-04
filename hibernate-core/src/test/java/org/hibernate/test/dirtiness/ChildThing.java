package org.hibernate.test.dirtiness;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
public class ChildThing implements CustomDirtyCheckable {
	@Id
	@GeneratedValue( generator = "increment" )
	@GenericGenerator( strategy = "increment", name = "increment" )
	private Long id;

	@ManyToOne()
	private Thing parent;

	@ManyToOne()
	private ChildThing nullableFKReference;

	public ChildThing() {
	}

	public ChildThing(Thing parent, ChildThing nullableFKReference) {
		this.parent = parent;
		this.nullableFKReference = nullableFKReference;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public Thing getParent() {
		return parent;
	}

	public void setParent(Thing parent) {
		this.parent = parent;
	}

	public ChildThing getNullableFKReference() {
		return nullableFKReference;
	}

	public void setNullableFKReference(ChildThing nullableFKReference) {
		this.nullableFKReference = nullableFKReference;
	}

	@Override
	public Map<String, Object> getChangedValues() {
		return changedValues;
	}

	@Transient
	private Map<String, Object> changedValues = new HashMap<String, Object>();
}
