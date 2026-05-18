/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.select;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class SelectionBook {
	@Id
	Long id;

	String title;

	int pages;

	LocalDate publishedOn;

	@Enumerated(EnumType.STRING)
	SelectionStatus status;

	@ManyToOne
	SelectionPublisher publisher;

	@ElementCollection
	List<String> tags;
}
