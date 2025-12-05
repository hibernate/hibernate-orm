/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.immutable;

import org.hibernate.annotations.Immutable;

import java.util.Objects;

/**
 * Created by soldier on 12.04.16.
 */
@Immutable
public class Caption {

	private String text;

	public Caption(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Caption caption = (Caption) o;
		return Objects.equals( text, caption.text );

	}

	@Override
	public int hashCode() {
		return text != null ? text.hashCode() : 0;
	}
}
