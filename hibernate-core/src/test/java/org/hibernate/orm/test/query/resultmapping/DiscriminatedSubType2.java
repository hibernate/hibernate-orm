/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultmapping;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * @author Steve Ebersole
 */
@Entity( name = "DiscriminatedSubType2" )
@DiscriminatorValue( "subtype2" )
class DiscriminatedSubType2 extends DiscriminatedRoot {
	private String subType2Name;

	public DiscriminatedSubType2() {
		super();
	}

	public DiscriminatedSubType2(Integer id, String rootName, String subType2Name) {
		super( id, rootName );
		this.subType2Name = subType2Name;
	}

	@Column( name = "subtype2_name" )
	public String getSubType2Name() {
		return subType2Name;
	}

	public void setSubType2Name(String subType2Name) {
		this.subType2Name = subType2Name;
	}
}
