/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.idclass;

import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface YourRepository
		extends BasicRepository<MyEntity, MyEntity.MyEntityId> {
	@Find
	List<MyEntity> findThem(@By("id(this)") MyEntity.MyEntityId id);
	@Find
	MyEntity findIt(@By("id(this)") MyEntity.MyEntityId id);

}
