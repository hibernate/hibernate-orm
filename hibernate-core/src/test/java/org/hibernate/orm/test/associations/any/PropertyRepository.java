/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations.any;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;

import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ManyToAny;

import static org.hibernate.annotations.CascadeType.ALL;

//tag::associations-many-to-any-example[]
@Entity
@Table(name = "property_repository")
public class PropertyRepository {

	@Id
	private Long id;

	@ManyToAny
	@AnyDiscriminator(DiscriminatorType.STRING)
	@Column(name = "property_type")
	@AnyKeyJavaClass(Long.class)
	@AnyDiscriminatorValue(discriminator = "S", entity = StringProperty.class)
	@AnyDiscriminatorValue(discriminator = "I", entity = IntegerProperty.class)
	@Cascade(ALL)
	@JoinTable(name = "repository_properties",
			joinColumns = @JoinColumn(name = "repository_id"),
			inverseJoinColumns = @JoinColumn(name = "property_id")
)
	private List<Property<?>> properties = new ArrayList<>();

	//Getters and setters are omitted for brevity

//end::associations-many-to-any-example[]
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public List<Property<?>> getProperties() {
		return properties;
	}

	//tag::associations-many-to-any-example[]
}
//end::associations-many-to-any-example[]
