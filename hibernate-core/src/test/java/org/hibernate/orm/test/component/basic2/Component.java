/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.basic2;


import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * {@inheritDoc}
 *
 * @author Mathieu Grenonville
 */
@Entity
public class Component {

	@Id
	private Long id;

	@Embedded
	private Component.Emb emb;

	@Access(AccessType.FIELD)
	@Embeddable
	public static class Emb {

		@OneToMany(targetEntity = Stuff.class)
		Set<Stuff> stuffs = new HashSet<Stuff>();

		@Entity
		@Table(name = "stuff")
		public static class Stuff {
			@Id
			private Long id;
		}
	}


}
