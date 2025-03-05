/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Emmanuel Bernard
 */
public class Competition {
	private Integer id;

	private List competitors = new ArrayList();


	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public List getCompetitors() {
		return competitors;
	}

	public void setCompetitors(List competitors) {
		this.competitors = competitors;
	}
}
