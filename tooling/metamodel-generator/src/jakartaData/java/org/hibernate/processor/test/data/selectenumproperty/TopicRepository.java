/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.selectenumproperty;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface TopicRepository extends DataRepository<Topic, Integer> {

	@Query("select topicId, topicStatus from Topic")
	List<TopicData> getTopicData();
}
