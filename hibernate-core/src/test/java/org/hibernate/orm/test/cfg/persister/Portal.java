/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cfg.persister;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Portal {
	@Id
	public Long getId() { return id; }
	public void setId(Long id) {  this.id = id; }
	private Long id;

	@OneToMany
	public Set<Window> getWindows() { return windows; }
	public void setWindows(Set<Window> windows) {  this.windows = windows; }
	private Set<Window> windows;
}
