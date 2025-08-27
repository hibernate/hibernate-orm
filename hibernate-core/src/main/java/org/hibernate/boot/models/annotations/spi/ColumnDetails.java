/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.spi;

/**
 * @author Steve Ebersole
 */
public interface ColumnDetails {
	String name();

	void name(String value);

	interface AlternateTableCapable {
		String table();

		void table(String value);
	}

	interface Nullable extends ColumnDetails {
		boolean nullable();

		void nullable(boolean value);
	}

	interface Mutable extends ColumnDetails {

		boolean insertable();

		void insertable(boolean value);

		boolean updatable();

		void updatable(boolean value);
	}

	interface Sizable extends ColumnDetails {
		int length();

		void length(int value);

		int precision();

		void precision(int value);

		int scale();

		void scale(int value);
	}

	interface SecondSizable extends Sizable {
		int secondPrecision();

		void secondPrecision(int value);
	}

	interface Uniqueable extends ColumnDetails {
		boolean unique();

		void unique(boolean value);
	}

	interface Definable extends ColumnDetails {
		String columnDefinition();

		void columnDefinition(String value);

		String options();

		void options(String value);
	}
}
