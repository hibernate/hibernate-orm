package org.hibernate.test.annotations.filter.subclass.singletable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Entity
@Table(name="ZOOLOGY_HUMAN")
@FilterDef(name="iqRange", parameters=
{
		@ParamDef(name="min", type="integer"),
		@ParamDef(name="max", type="integer")
})
@Filter(name="iqRange", condition="HUMAN_IQ BETWEEN :min AND :max")
public class Human extends Mammal {
	@Column(name="HUMAN_IQ")
	private int iq;
	
	public int getIq() {
		return iq;
	}

	public void setIq(int iq) {
		this.iq = iq;
	}

}
