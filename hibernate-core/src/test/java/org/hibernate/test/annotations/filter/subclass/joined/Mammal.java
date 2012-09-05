package org.hibernate.test.annotations.filter.subclass.joined;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Type;

@Entity
@Table(name="ZOOLOGY_MAMMAL")
@FilterDef(name="pregnantOnly")
@Filter(name="pregnantOnly", condition="IS_PREGNANT = 1")
public class Mammal extends Animal{
	
	@Column(name="IS_PREGNANT")
	@Type( type="numeric_boolean" )
	private boolean isPregnant;

	public boolean isPregnant() {
		return isPregnant;
	}

	public void setPregnant(boolean isPregnant) {
		this.isPregnant = isPregnant;
	}
}
