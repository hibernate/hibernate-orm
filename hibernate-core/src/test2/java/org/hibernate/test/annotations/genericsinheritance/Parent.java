/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.genericsinheritance;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;

@MappedSuperclass
public abstract class Parent<C extends Child> {

	@Id @GeneratedValue Long id;
	@MapKey @OneToMany(mappedBy="parent") Map<Long,C> children = new HashMap<Long,C>();

	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	
	public Map<Long,C> getChildren() {
		return children;
	}
	public void setChildren(Map<Long,C> children) {
		this.children = children;
	}
	
	
}
