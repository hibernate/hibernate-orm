/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mutability;

import java.time.Instant;
import java.util.Date;

import org.hibernate.Session;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Used as example entity in UG
 *
 * @author Steve Ebersole
 */
//tag::mutability-base-entity-example[]
@Entity
public class MutabilityBaselineEntity {
	@Id
	private Integer id;
	@Basic
	private String name;
	@Basic
	private Date activeTimestamp;
//end::mutability-base-entity-example[]

	private MutabilityBaselineEntity() {
		// for Hibernate use
	}

	public MutabilityBaselineEntity(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getActiveTimestamp() {
		return activeTimestamp;
	}

	public void setActiveTimestamp(Date activeTimestamp) {
		this.activeTimestamp = activeTimestamp;
	}

	void stringExample() {
		//tag::mutability-base-string-example[]
		Session session = getSession();
		MutabilityBaselineEntity entity = session.find( MutabilityBaselineEntity.class, 1 );
		entity.setName( "new name" );
		//end::mutability-base-string-example[]
	}

	private void dateExampleSet() {
		//tag::mutability-base-date-set-example[]
		Session session = getSession();
		MutabilityBaselineEntity entity = session.find( MutabilityBaselineEntity.class, 1 );
		entity.setActiveTimestamp( now() );
		//end::mutability-base-date-set-example[]
	}

	private void dateExampleMutate() {
		//tag::mutability-base-date-mutate-example[]
		Session session = getSession();
		MutabilityBaselineEntity entity = session.find( MutabilityBaselineEntity.class, 1 );
		entity.getActiveTimestamp().setTime( now().getTime() );
		//end::mutability-base-date-mutate-example[]
	}

	private Session getSession() {
		return null;
	}

	private Date now() {
		return Date.from( Instant.now() );
	}

//tag::mutability-base-entity-example[]
}
//end::mutability-base-entity-example[]
