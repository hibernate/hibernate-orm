/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.inheritance.hhh_x;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import java.util.List;

/**
 * @author Andrea Boriero
 */
@DiscriminatorValue("group")
@Entity
public class GroupStep extends Step {

	@OneToMany(mappedBy = "parent")
	private List<Step> steps;
}
