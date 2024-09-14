/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.resultmapping;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * @author Steve Ebersole
 */
@Entity( name = "DiscriminatedSubType1" )
@DiscriminatorValue( "subtype1" )
class DiscriminatedSubType1 extends DiscriminatedRoot {
	private String subType1Name;

	public DiscriminatedSubType1() {
		super();
	}

	public DiscriminatedSubType1(Integer id, String rootName, String subType1Name) {
		super( id, rootName );
		this.subType1Name = subType1Name;
	}

	@Column( name = "subtype1_name" )
	public String getSubType1Name() {
		return subType1Name;
	}

	public void setSubType1Name(String subType1Name) {
		this.subType1Name = subType1Name;
	}
}
