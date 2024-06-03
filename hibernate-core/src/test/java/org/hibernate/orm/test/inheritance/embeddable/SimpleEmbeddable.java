/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance.embeddable;

import org.hibernate.annotations.Imported;

import jakarta.persistence.Embeddable;

/**
 * @author Marco Belladelli
 */
@Embeddable
public class SimpleEmbeddable {
	private String data;

	public SimpleEmbeddable() {
	}

	public SimpleEmbeddable(String data) {
		this.data = data;
	}

	public String getData() {
		return data;
	}
}
