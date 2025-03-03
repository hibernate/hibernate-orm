/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.usertype.json;

import java.util.Objects;

/**
 * @author Vlad Mihalcea
 */
public class Json {

	private final String content;

	public Json(String content) {
		this.content = content;
	}

	public String getContent() {
		return content;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		return Objects.equals( content, Json.class.cast( o ).content );
	}

	@Override
	public int hashCode() {
		return Objects.hash( content );
	}

	@Override
	public String toString() {
		return content;
	}
}
