/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.processingorder;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import java.util.List;

@Entity
public class Topic {
	@Id
	Integer id;

	@OneToMany(mappedBy = "topic", targetEntity = Post.class)
	@OrderBy
	private List<Post> posts;

}
