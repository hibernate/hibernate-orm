/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e3.b;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name="`Dependent`")
public class Dependent {
	// default column name for "name" attribute is overridden
	@AttributeOverride(name = "name", column = @Column(name = "dep_name"))
	@EmbeddedId
	DependentId id;


	@MapsId("empPK")
	@JoinColumn(name = "FK1", referencedColumnName = "FIRSTNAME")
	@JoinColumn(name = "FK2", referencedColumnName = "lastName")
	@ManyToOne
	Employee emp;

}
