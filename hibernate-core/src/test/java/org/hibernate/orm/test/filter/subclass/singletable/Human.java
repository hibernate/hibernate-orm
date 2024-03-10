/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.filter.subclass.singletable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Entity
@FilterDef(name="iqRange", parameters=
{
		@ParamDef(name="min", type=Integer.class),
		@ParamDef(name="max", type=Integer.class)
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
