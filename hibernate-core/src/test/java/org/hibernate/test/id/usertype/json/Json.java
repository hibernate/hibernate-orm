/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.id.usertype.json;

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
