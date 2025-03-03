/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.idclass;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

@Entity
@IdClass(MyEntity.MyEntityId.class)
public class MyEntity {

	@Id
	Integer topicId;

	@Id
	String userId;

	String status;

	public record MyEntityId(Integer topicId, String userId) {

	}
}
