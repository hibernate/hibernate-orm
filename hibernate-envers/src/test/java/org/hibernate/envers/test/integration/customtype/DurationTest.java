/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.customtype;


import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import java.time.Duration;

@JiraKey(value = "HHH-17243")
public class DurationTest extends BaseEnversJPAFunctionalTestCase{

	@Entity(name = "Duration")
	@Audited
	public static class DurationTestEntity {
		@Id
		@GeneratedValue
		private Integer id;

		private Duration duration;

				DurationTestEntity(){

				}

		DurationTestEntity(Duration aDuration) {
					this.duration = aDuration;
		}

		public Integer getId() {
			return id;
		}

		public Duration getDuration() {
			return duration;
		}

		public void setDuration(Duration aDuration) {
			this.duration = aDuration;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { DurationTestEntity.class };
	}

	private Integer durationId;

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1 - insert
		this.durationId = doInJPA( this::entityManagerFactory, entityManager -> {
			final DurationTestEntity duration = new DurationTestEntity(Duration.ofHours(2));
			entityManager.persist( duration );
			return duration.getId();
		} );

		// Revision 2 - update
		doInJPA( this::entityManagerFactory, entityManager -> {
			final DurationTestEntity duration = entityManager.find( DurationTestEntity.class, this.durationId );
			duration.setDuration(Duration.ofHours(3));
			entityManager.merge(duration);
		} );
	}
}
