/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metadata;
import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;


/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name="ejb_parent")
public class Parent {

	@Embeddable
	public static class Relatives<T extends Being> {
		private Set<T> siblings;

		@OneToMany
		@JoinColumn(name="siblings_fk")
		public Set<T> getSiblings() {
			return siblings;
		}

		public void setSiblings(Set<T> siblings) {
			this.siblings = siblings;
		}
	}

	private Integer id;
	private String name;
	private Set<Child> children;
	private Relatives<Child> siblings;


	@Id
	@GeneratedValue
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

	@OneToMany
	@JoinColumn(name="parent_fk", nullable = false)
	public Set<Child> getChildren() {
		return children;
	}

	public void setChildren(Set<Child> children) {
		this.children = children;
	}

	//@Transient
	@Embedded
	public Relatives<Child> getSiblings() {
		return siblings;
	}

	public void setSiblings(Relatives<Child> siblings) {
		this.siblings = siblings;
	}
}
