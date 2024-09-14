/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.softdelete.secondary;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
//tag::example-soft-delete-secondary[]
@Entity
@Table(name = "joined_sub")
@PrimaryKeyJoinColumn(name = "joined_fk")
public class JoinedSub extends JoinedRoot {
	// ...
//end::example-soft-delete-secondary[]
	@Basic
	String subDetails;

	public JoinedSub() {
	}

	public JoinedSub(Integer id, String name, String subDetails) {
		super( id, name );
		this.subDetails = subDetails;
	}
//tag::example-soft-delete-secondary[]
}
//end::example-soft-delete-secondary[]
