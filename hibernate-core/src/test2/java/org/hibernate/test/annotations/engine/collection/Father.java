/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.engine.collection;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "co_father")
public class Father {
	@Id
	@GeneratedValue
	public Integer getId() { return id; }
	public void setId(Integer id) {  this.id = id; }
	private Integer id;

	@OneToMany
	@OrderColumn(name = "son_arriv")
	@JoinColumn(name = "father_id", nullable = false)
	@Cascade({ CascadeType.SAVE_UPDATE })
	public List<Son> getOrderedSons() { return orderedSons; }
	public void setOrderedSons(List<Son> orderedSons) {  this.orderedSons = orderedSons; }
	private List<Son> orderedSons = new ArrayList<Son>( );
}
