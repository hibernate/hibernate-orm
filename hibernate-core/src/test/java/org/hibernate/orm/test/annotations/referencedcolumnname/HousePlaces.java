/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.referencedcolumnname;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

/**
 * @author Janario Oliveira
 */
@Entity
public class HousePlaces {

	@Id
	@GeneratedValue
	int id;
	@Embedded
	Places places;
	@Embedded
	@AssociationOverrides({
			@AssociationOverride(name = "livingRoom", joinColumns = {
					@JoinColumn(name = "NEIGHBOUR_LIVINGROOM", referencedColumnName = "NAME"),
					@JoinColumn(name = "NEIGHBOUR_LIVINGROOM_OWNER", referencedColumnName = "OWNER") }),
			@AssociationOverride(name = "kitchen", joinColumns = @JoinColumn(name = "NEIGHBOUR_KITCHEN", referencedColumnName = "NAME")) })
	Places neighbourPlaces;
}
