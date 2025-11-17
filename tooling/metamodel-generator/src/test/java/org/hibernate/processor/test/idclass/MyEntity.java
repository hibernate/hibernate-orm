/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.idclass;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.NamedQuery;
import org.hibernate.annotations.processing.CheckHQL;

@Entity
@IdClass(MyEntity.MyEntityId.class)
@CheckHQL
@NamedQuery(name = "#findById", query = "from MyEntity e where e.id=:id")
public class MyEntity {

	@Id
	Integer topicId;

	@Id
	String userId;

	String status;

	public record MyEntityId(Integer topicId, String userId) {

	}
}
