/**
 * 
 */
package org.hibernate.envers.test.entities.collection;

import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.entities.customtype.Component;
import org.hibernate.envers.test.entities.customtype.CompositeTestUserType;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@Entity
@TypeDef(name = "comp", typeClass = CompositeTestUserType.class)
public class CompositeCustomTypeSetEntity {

	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	@ElementCollection
	@Type(type = "comp")
	@CollectionTable(name = "components", joinColumns = @JoinColumn(name = "entity_id"))
	@Columns(columns = { @Column(name = "str", nullable = true), @Column(name = "num", nullable = true) })
	private Set<Component> components;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the components
	 */
	public Set<Component> getComponents() {
		return components;
	}

	/**
	 * @param components the components to set
	 */
	public void setComponents(Set<Component> components) {
		this.components = components;
	}

}
