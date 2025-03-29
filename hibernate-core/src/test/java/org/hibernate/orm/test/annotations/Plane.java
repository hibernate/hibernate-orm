/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;

/**
 * Plane class
 *
 * @author Emmanuel Bernard
 */
@Entity()
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "planetype", length = 100, discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("Plane")
@AttributeOverride(name = "altitude", column = @Column(name = "fld_altitude"))
@NamedQuery(name = "plane.byId", query = "from Plane where id = :id",
		hints = {@QueryHint(name = "org.hibernate.cacheable", value = "true"),
		@QueryHint(name = "org.hibernate.cacheRegion", value = "testedCacheRegion"),
		@QueryHint(name = "org.hibernate.timeout", value = "100"),
		@QueryHint(name = "org.hibernate.fetchSize", value = "1"),
		@QueryHint(name = "org.hibernate.flushMode", value = "Commit"),
		@QueryHint(name = "org.hibernate.cacheMode", value = "NORMAL"),
		@QueryHint(name = "org.hibernate.comment", value = "Plane by id")})
public class Plane extends FlyingObject {

	private Long id;
	private int nbrofSeats;

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public int getNbrOfSeats() {
		return nbrofSeats;
	}

	public void setId(Long long1) {
		id = long1;
	}

	public void setNbrOfSeats(int i) {
		nbrofSeats = i;
	}

}
