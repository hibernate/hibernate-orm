/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetomany;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity(name="Forum")
public class Forum{

	private Long id;
	private String name;
	protected List<Comment> posts = new ArrayList<Comment>();
	protected List<User> users = new ArrayList<User>();
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", updatable = false, insertable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	@OneToMany(mappedBy = "forum", cascade = CascadeType.ALL , orphanRemoval = false, fetch = FetchType.LAZY)
	@LazyCollection(LazyCollectionOption.EXTRA)
	@OrderColumn(name = "idx2")
	public List<Comment> getPosts() {
		return posts;
	}

	public void setPosts(List<Comment> children) {
		this.posts = children;
	}
	
	@OneToMany(mappedBy = "forum", cascade = CascadeType.ALL , orphanRemoval = true, fetch = FetchType.LAZY)
	@LazyCollection(LazyCollectionOption.EXTRA)
	@OrderColumn(name = "idx3")
	public List<User> getUsers() {
		return users;
	}

	public void setUsers(List<User> users) {
		this.users = users;
	}

	@Column
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
