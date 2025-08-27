/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.contribution.array; /**
 * @author Steve Ebersole
 */

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;

@Entity(name = "Post")
@Table(name = "Post")
public class Post {
	@Id
	public Integer id;
	@Basic
	public String title;
	@Basic
	public String[] tags;

	private Post() {
		// for Hibernate use
	}

	public Post(Integer id, String title) {
		this.id = id;
		this.title = title;
	}

	public Post(Integer id, String title, String... tags) {
		this.id = id;
		this.title = title;
		this.tags = tags;
	}

	public Integer getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String[] getTags() {
		return tags;
	}

	public void setTags(String[] tags) {
		this.tags = tags;
	}
}
