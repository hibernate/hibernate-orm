/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.version;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Version;

/**
 * @author Steve Ebersole
 */
@Entity
public class Customer {
	@Id
	public Long id;

	@OneToMany( fetch = FetchType.EAGER, mappedBy = "customer", cascade = CascadeType.ALL )
	public List<Order> orders = new ArrayList<Order>();

	@Version
	public long version;
}
