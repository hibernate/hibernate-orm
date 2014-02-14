package org.hibernate.envers.test.integration.components;

import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.entities.components.Component1;

@Entity
@Table(name = "CompTestEntities")
public class CollectionOfComponentTestEntity {

	@Id
	@GeneratedValue
	private Integer id;

	@ElementCollection
	@CollectionTable(name = "CompTestEntityComponents", joinColumns = @JoinColumn(name = "entity_id"))
	@Audited
	private Set<Component1> comp1s;

	public CollectionOfComponentTestEntity() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<Component1> getComp1s() {
		return comp1s;
	}

	public void setComp1s(Set<Component1> comp1s) {
		this.comp1s = comp1s;
	}

}
