/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.userguide.util;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;


/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public abstract class CopyrightableContent {
	private Author author;

	public CopyrightableContent() {
	}

	public CopyrightableContent(Author author) {
		this.author = author;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	private Author getAuthor() {
		return author;
	}

	private void setAuthor(Author author) {
		this.author = author;
	}
}
