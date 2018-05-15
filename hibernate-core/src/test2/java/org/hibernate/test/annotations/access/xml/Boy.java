/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id:$

package org.hibernate.test.annotations.access.xml;
import java.util.Set;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Entity;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Boy {
	@Id
	@GeneratedValue
	private long id;

	private Set<String> nickNames;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Set<String> getNickNames() {
		return nickNames;
	}

	public void setNickNames(Set<String> nickNames) {
		this.nickNames = nickNames;
	}
}


