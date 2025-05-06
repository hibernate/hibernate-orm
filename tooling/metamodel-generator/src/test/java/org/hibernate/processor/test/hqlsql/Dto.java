/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hqlsql;

import java.util.Objects;

public final class Dto {
	private final String title;
	private final int pages;

	public Dto(String title, int pages) {
		this.title = title;
		this.pages = pages;
	}

	public String title() {
		return title;
	}

	public int pages() {
		return pages;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (Dto) obj;
		return Objects.equals(this.title, that.title) &&
				this.pages == that.pages;
	}

	@Override
	public int hashCode() {
		return Objects.hash(title, pages);
	}

	@Override
	public String toString() {
		return "Dto[" +
				"title=" + title + ", " +
				"pages=" + pages + ']';
	}

}
