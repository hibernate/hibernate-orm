/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.parsed.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Bag;
import org.hibernate.annotations.NamedEntityGraph;

import java.util.Collection;

/**
 * @author Steve Ebersole
 */
@Entity(name = "PublishingHouse")
@Table(name = "PublishingHouse")
@Inheritance(strategy = InheritanceType.JOINED)
@NamedEntityGraph(name = "publishing-house-bio", graph = "name, ceo, boardMembers")
public class PublishingHouse {
	@Id
	private Integer id;
	private String name;

	@ManyToOne
	@JoinColumn(name = "ceo_fk")
	private Person ceo;

	@OneToMany
	@Bag
	@JoinColumn(name = "board_mem_fk")
	private Collection<Person> boardMembers;

	@OneToMany
	@Bag
	@JoinColumn(name = "ceo_fk")
	private Collection<Publisher> publishers;

	@OneToMany
	@Bag
	@JoinColumn(name = "editor_fk")
	private Collection<Person> editors;
}
