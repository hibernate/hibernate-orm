/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.options;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Owning side of {@linkplain Article#review}, joined on a foreign-key column.
 *
 * @author Ivo Raisr
 */
@Entity
@Table(name = "article_reviews")
public class ArticleReview {
	@Id
	private Integer id;
	private String assessment;

	@OneToOne
	@JoinColumn(name = "article_fk")
	private Article article;

	public ArticleReview() {
	}

	public ArticleReview(Integer id, String assessment, Article article) {
		this.id = id;
		this.assessment = assessment;
		this.article = article;
	}
}
