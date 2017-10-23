/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class User {
	private Long id;
	private String username;
	private Set memberships = new HashSet();

	/**
	 * for persistence
	 */
	User() {
	}

	public User(String username) {
		this.username = username;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public Iterator getMemberships() {
		return memberships.iterator();
	}

	public Membership addMembership(Group group) {
		Membership membership = new Membership( this, group );
		memberships.add( membership );
		return membership;
	}
}
