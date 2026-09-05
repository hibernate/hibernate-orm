/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.options;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Holds an inverse one-to-one, which contributes no column to {@code articles}: the join
 * column belongs to {@code article_reviews}.
 *
 * @author Ivo Raisr
 */
@Entity
@Table(name = "articles")
public class Article {
	@Id
	private Integer id;
	private String title;

	@OneToOne(mappedBy = "article")
	private ArticleReview review;

	public Article() {
	}

	public Article(Integer id, String title) {
		this.id = id;
		this.title = title;
	}
}
