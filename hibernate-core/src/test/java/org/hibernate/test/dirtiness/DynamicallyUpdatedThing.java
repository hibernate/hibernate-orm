package org.hibernate.test.dirtiness;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;

@Entity
@DynamicUpdate()
public class DynamicallyUpdatedThing implements CustomDirtyCheckable {
	@Id
	@GeneratedValue( generator = "increment" )
	@GenericGenerator( strategy = "increment", name = "increment" )
	private Long id;

	private String name;
	private Date mutableProperty;

	public DynamicallyUpdatedThing() {
	}

	public DynamicallyUpdatedThing(String name) {
		this.name = name;
	}

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
		// intentionally simple dirty tracking (i.e. no checking against previous state)
		changedValues.put( "name", this.name );
		this.name = name;
	}

	public Date getMutableProperty() {
		return mutableProperty;
	}

	public void setMutableProperty(Date mutableProperty) {
		// intentionally simple dirty tracking (i.e. no checking against previous state)
		changedValues.put( "mutableProperty", this.mutableProperty );
		this.mutableProperty = mutableProperty;
	}

	@Override
	public Map<String, Object> getChangedValues() {
		return changedValues;
	}

	@Transient
	private Map<String, Object> changedValues = new HashMap<String, Object>();
}
