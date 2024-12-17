/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.association;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;


@MappedSuperclass
abstract class Document {

	@ManyToOne
	protected Person author;

	@ManyToOne
	private Person translator;

	public Person getAuthor() {
		return this.author;
	}

	public void setAuthor(Person author) {
		this.author = author;
	}

	public Person getTranslator() {
		return this.translator;
	}

	public void setTranslator(Person translator) {
		this.translator = translator;
	}
}
