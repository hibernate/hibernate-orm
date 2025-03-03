/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.collections;

import java.util.Collection;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.type.NumericBooleanConverter;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "invalid_coll_owner")
public class InvalidCollectionOwner {
	@Id
	private Integer id;
	@Basic
	private String name;

	@OneToMany
	@JoinColumn(name="owned_fk")
	@SoftDelete(columnName = "gone", converter = NumericBooleanConverter.class)
	private Collection<CollectionOwned> oneToMany;
}
