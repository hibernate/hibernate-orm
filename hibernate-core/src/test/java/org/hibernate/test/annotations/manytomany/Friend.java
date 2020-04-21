/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.manytomany;
import java.io.Serializable;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;


/**
 * Friend has other friends in a many to many way
 *
 * @author Emmanuel Bernard
 */
@Entity()
public class Friend implements Serializable {
	private Integer id;
	private String name;
	private Set<Friend> friends;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setId(Integer integer) {
		id = integer;
	}

	public void setName(String string) {
		name = string;
	}

	@ManyToMany(
			cascade = {CascadeType.PERSIST, CascadeType.MERGE}
	)
	@JoinTable(
			name = "FRIEND2FRIEND",
			joinColumns = {@JoinColumn(name = "FROM_FR", nullable = false)},
			inverseJoinColumns = {@JoinColumn(name = "TO_FR", nullable = false)}
	)
	public Set<Friend> getFriends() {
		return friends;
	}

	public void setFriends(Set<Friend> friend) {
		this.friends = friend;
	}
}
