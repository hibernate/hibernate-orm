package org.hibernate.test.annotations.filter.subclass.joined;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Entity
@Inheritance(strategy=InheritanceType.JOINED)
@Table(name="ZOOLOGY_ANIMAL")
@FilterDef(name="ignoreSome", parameters={@ParamDef(name="name", type="string")})
@Filter(name="ignoreSome", condition=":name <> ANIMAL_NAME")
public class Animal {
	@Id
	@GeneratedValue
	@Column(name="ANIMAL_ID")
	private Integer id;
	
	@Column(name="ANIMAL_NAME")
	private String name;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
