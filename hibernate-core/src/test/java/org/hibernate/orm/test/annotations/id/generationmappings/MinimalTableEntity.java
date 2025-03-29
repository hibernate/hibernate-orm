/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.generationmappings;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
@Table( name = "MINIMAL_TBL" )
public class MinimalTableEntity {
	public static final String TBL_NAME = "minimal_tbl";

	private Long id;

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "MINIMAL_TBL")
	@TableGenerator( name = "MINIMAL_TBL", table = TBL_NAME )
	public Long getId() {
		return id;
	}

	public void setId(Long long1) {
		id = long1;
	}
}
