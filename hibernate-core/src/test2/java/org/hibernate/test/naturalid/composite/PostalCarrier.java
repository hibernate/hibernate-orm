/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.naturalid.composite;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.NaturalId;

/**
 * @author Steve Ebersole
 */
@Entity(name = "PostalCarrier")
public class PostalCarrier {
	@Id
	private Long id;

	@NaturalId
	@Embedded
	private PostalCode postalCode;

	public PostalCarrier() {
	}

	public PostalCarrier(long id, PostalCode postalCode) {
		this.id = id;
		this.postalCode = postalCode;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public PostalCode getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(PostalCode postalCode) {
		this.postalCode = postalCode;
	}
}
