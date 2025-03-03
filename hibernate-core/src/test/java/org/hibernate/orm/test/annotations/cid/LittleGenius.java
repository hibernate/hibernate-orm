/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;

/**
 * Hierarchy with cid + many to one
 * @author Anthony
 *
 */
@Entity
@PrimaryKeyJoinColumns({
@PrimaryKeyJoinColumn(name = "nthChild"),
@PrimaryKeyJoinColumn(name = "parentLastName"),
@PrimaryKeyJoinColumn(name = "parentFirstName")})
public class LittleGenius extends Child {
	public String particularSkill;
}
