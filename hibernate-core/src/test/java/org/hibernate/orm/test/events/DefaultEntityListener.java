/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.events;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * @author Vlad Mihalcea
 */
//tag::events-default-listener-mapping-example[]
public class DefaultEntityListener {

	public void onPersist(Object entity) {
		if (entity instanceof BaseEntity) {
			BaseEntity baseEntity = (BaseEntity) entity;
			baseEntity.setCreatedOn(now());
		}
	}

	public void onUpdate(Object entity) {
		if (entity instanceof BaseEntity) {
			BaseEntity baseEntity = (BaseEntity) entity;
			baseEntity.setUpdatedOn(now());
		}
	}

	private Timestamp now() {
		return Timestamp.from(
			LocalDateTime.now().toInstant(ZoneOffset.UTC)
	);
	}
}
//end::events-default-listener-mapping-example[]
