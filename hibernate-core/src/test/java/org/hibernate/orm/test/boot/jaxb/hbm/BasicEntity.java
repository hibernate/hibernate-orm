/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.hbm;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public class BasicEntity {
	private Integer id;
	private String name;
	private BasicComposition composition;
	private BasicEntity another;
	private List<BasicEntity> others;

	private BasicEntity() {
		// for Hibernate use
	}

	public BasicEntity(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
