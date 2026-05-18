/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.metamodel;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class DataMetamodelEntity {
	@Id
	Long id;

	String title;

	boolean published;

	int pages;

	BigDecimal price;

	LocalDate publicationDate;

	DataMetamodelStatus status;

	Object payload;

	byte[] bytes;

	@Embedded
	DataMetamodelPart part;

	@ManyToOne
	DataMetamodelRelated related;
}
