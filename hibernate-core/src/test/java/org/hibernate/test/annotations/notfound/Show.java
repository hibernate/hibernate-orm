/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.notfound;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToOne;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Show {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@OneToOne()
	@NotFound(action = NotFoundAction.IGNORE )
	@JoinTable(name = "Show_Description",
			joinColumns = @JoinColumn(name = "show_id"),
			inverseJoinColumns = @JoinColumn(name = "description_id"))
	private ShowDescription description;


	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public ShowDescription getDescription() {
		return description;
	}

	public void setDescription(ShowDescription description) {
		this.description = description;
	}
}
