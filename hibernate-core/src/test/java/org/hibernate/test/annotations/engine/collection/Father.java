/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.engine.collection;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

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
