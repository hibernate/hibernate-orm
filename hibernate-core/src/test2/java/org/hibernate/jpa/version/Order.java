/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.version;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * @author Steve Ebersole
 */
@Entity
@Table( name = "T_ORDER" )
public class Order {
	@Id
	public Long id;

	@ManyToOne( cascade = CascadeType.ALL )
	public Customer customer;

	@Version
	public long version;
}
