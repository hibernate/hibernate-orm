/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.graphs.mapped_by_id;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;


/**
 * @author Oliver Breidenbach
 */
@Entity
public class User {

	@Id
	public Long id;

	public String name;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id", referencedColumnName = "id", insertable = false, updatable = false)
	public UserStatistic userStatistic;

}
