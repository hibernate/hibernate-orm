/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author tknowlton at iamhisfriend dot org
 */
@Entity
public class Item implements Serializable {
	@Id
	String name;

	@OneToMany(mappedBy = "item", fetch = FetchType.EAGER)
	@OrderBy("amount desc")
	Set<Bid> bids = new HashSet<Bid>();
}
