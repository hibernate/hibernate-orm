/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.retail;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * @author Steve Ebersole
 */
@Entity
@DiscriminatorValue( "foreign" )
public class ForeignVendor extends Vendor {
	public ForeignVendor() {
	}

	public ForeignVendor(Integer id, String name, String billingEntity) {
		super( id, name, billingEntity );
	}
}
