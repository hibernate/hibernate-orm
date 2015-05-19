/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.annotations.manytoonewithformula;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;

/**
 * @author Sharath Reddy
 */
@Entity
public class Company implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private int id;
	private Person person;
	
	@Id @GeneratedValue
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	@ManyToOne
	@JoinColumnsOrFormulas(
	{ 
		@JoinColumnOrFormula(column=@JoinColumn(name="id", referencedColumnName="company_id", updatable=false, insertable=false)),
		@JoinColumnOrFormula(formula=@JoinFormula(value="'T'", referencedColumnName="is_default"))
	})
	public Person getDefaultContactPerson() {
		return person;
	}
	public void setDefaultContactPerson(Person person) {
		this.person = person;
	}
	
}
