/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.arraytype;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashSet;
import java.util.Set;

/**
 * Test entity for HHH-20386: Set<&lt;>String&gt; with {@code @JdbcTypeCode(SqlTypes.ARRAY)}
 * should generate {@code SingularAttribute<JdbcTypeCodeArrayEntity, Set<String>>}
 * (not the raw type {@code SingularAttribute<JdbcTypeCodeArrayEntity, Set>}).
 */
@Entity
public class JdbcTypeCodeArrayEntity {
	@Id
	private Long id;

	@Column
	@JdbcTypeCode(SqlTypes.ARRAY)
	private Set<String> tags = new HashSet<>();

	@Column
	private Set<String> tagsWithoutAnnotation = new HashSet<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}

	public Set<String> getTagsWithoutAnnotation() {
		return tagsWithoutAnnotation;
	}

	public void setTagsWithoutAnnotation(Set<String> tagsWithoutAnnotation) {
		this.tagsWithoutAnnotation = tagsWithoutAnnotation;
	}
}
