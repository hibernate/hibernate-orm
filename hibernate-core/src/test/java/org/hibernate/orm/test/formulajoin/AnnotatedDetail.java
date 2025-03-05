/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.formulajoin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
@Entity
public class AnnotatedDetail {
	@Id
	private Integer id;
	private String name;

	// because otherwise schema export would not know about it...
	@Column( name = "detail_domain" )
	private String domain;
}
