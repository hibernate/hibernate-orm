/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.engine.collection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name="co_son")
public class Son {
	@Id
	@GeneratedValue
	public Integer getId() { return id; }
	public void setId(Integer id) {  this.id = id; }
	private Integer id;

	@ManyToOne(optional = false) @JoinColumn(name = "father_id", insertable = false, updatable = false, nullable = false)
	public Father getFather() { return father; }
	public void setFather(Father father) {  this.father = father; }
	private Father father;

	@ManyToOne
	public Mother getMother() { return mother; }
	public void setMother(Mother mother) {  this.mother = mother; }
	private Mother mother;
}
