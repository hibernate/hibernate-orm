/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
@Entity
public class CrazyHqlKeywordEntity {
	@Id
	public Integer id;

	public String name;

	public String from;
	public String select;
	public String order;
}
