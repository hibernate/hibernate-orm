/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.fetch.depth;

import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_mod")
public class SysModule2 {
	@Id
	private Integer id;

	@Column(name = "name")
	private String name;

	@OneToMany( cascade = CascadeType.PERSIST, fetch = FetchType.EAGER )
	@JoinColumn( name = "target_mod_fk" )
	private Set<SysModule2> targetModules;
}
