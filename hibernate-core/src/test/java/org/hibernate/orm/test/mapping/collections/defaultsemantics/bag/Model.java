/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.defaultsemantics.bag;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import org.hibernate.annotations.Bag;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.annotations.SQLOrder;

/// @author Steve Ebersole
public class Model {
	@Entity(name = "Owner")
	public static class Owner {
		@Id public Integer id;
		@ElementCollection public List<String> plain = new ArrayList<>();
		@ElementCollection @Bag public List<String> bag;
		@ElementCollection @OrderColumn public List<String> indexed;
		@ElementCollection @ListIndexBase(1) public List<String> based;
		@ElementCollection @OrderBy public List<String> ordered;
		@ElementCollection @SQLOrder("value_column") @Column(name = "value_column")
		public List<String> sqlOrdered;
		@ElementCollection
		@CollectionId(column = @Column(name = "row_id"), generator = "increment")
		@CollectionIdJdbcTypeCode(Types.BIGINT)
		public List<String> identified;
		@OneToMany(mappedBy = "owner") public List<Child> inverse;
		@OneToMany(mappedBy = "indexedOwner") @OrderColumn public List<Child> inverseIndexed;
		@ManyToMany(mappedBy = "owners") public List<Child> inverseMany;
	}

	@Entity(name = "Child")
	public static class Child {
		@Id public Integer id;
		@ManyToOne public Owner owner;
		@ManyToOne public Owner indexedOwner;
		@ManyToMany public List<Owner> owners;
	}

	@MappedSuperclass
	public static class Base {
		@Id public Integer id;
		@ElementCollection public List<String> inherited;
	}

	@Embeddable
	public static class Details {
		@ElementCollection public List<String> embedded;
	}

	@Entity(name = "InvalidOrder")
	public static class InvalidOrder {
		@Id public Integer id;
		@ElementCollection @Bag @OrderColumn public List<String> values;
	}

	@Entity(name = "InvalidBase")
	public static class InvalidBase {
		@Id public Integer id;
		@ElementCollection @Bag @ListIndexBase(1) public List<String> values;
	}
}
