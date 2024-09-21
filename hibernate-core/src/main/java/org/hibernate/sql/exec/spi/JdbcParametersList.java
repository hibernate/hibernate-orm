/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import java.util.List;

import org.hibernate.sql.ast.tree.expression.JdbcParameter;

/**
 * Conceptually similar to a List of JdbcParameters, but exposing a
 * read-only immutable contract.
 * Also as nice side effect, avoid any potential type pollution
 * problems during access.
 */
public interface JdbcParametersList {

	static final JdbcParametersList EMPTY = new JdbcParametersListMulti( new JdbcParameter[]{} );

	public JdbcParameter get(int selectionIndex);

	public int size();

	public static Builder newBuilder() {
		return newBuilder( 2 );
	}

	public static JdbcParametersList fromList(final List<JdbcParameter> originalList) {
		final Builder builder = newBuilder( originalList.size() );
		for ( JdbcParameter element : originalList ) {
			builder.add( element );
		}
		return builder.build();
	}

	public static JdbcParametersList empty() {
		return EMPTY;
	}

	public static JdbcParametersList singleton(final JdbcParameter p) {
		return new JdbcParametersListSingleton( p );
	}

	public static Builder newBuilder(final int i) {
		return new Builder( i );
	}

	public static class Builder {
		private JdbcParameter[] array;
		private int index = 0;

		private Builder(final int sizeEstimate) {
			this.array = new JdbcParameter[sizeEstimate];
		}

		public void add(final JdbcParameter jdbcParameter) {
			if ( index >= array.length ) {
				int newSize = Math.max( index + 2, array.length >> 1 );
				JdbcParameter[] newArray = new JdbcParameter[newSize];
				System.arraycopy( array, 0, newArray, 0, array.length );
				this.array = newArray;
			}
			this.array[index++] = jdbcParameter;
		}

		public JdbcParametersList build() {
			if ( index == 0 ) {
				return EMPTY;
			}
			else if ( index == 1 ) {
				return singleton( array[0] );
			}
			else if ( index == array.length ) {
				return new JdbcParametersListMulti( array );
			}
			else {
				JdbcParameter[] newArray = new JdbcParameter[index];
				System.arraycopy( array, 0, newArray, 0, index );
				return new JdbcParametersListMulti( newArray );
			}
		}
	}

	public final class JdbcParametersListMulti implements JdbcParametersList {

		private final JdbcParameter[] array;

		private JdbcParametersListMulti(JdbcParameter[] inputArray) {
			this.array = inputArray;
		}

		public JdbcParameter get(int selectionIndex) {
			return array[selectionIndex];
		}

		public int size() {
			return array.length;
		}
	}

	public final class JdbcParametersListSingleton implements JdbcParametersList {

		private final JdbcParameter singleElement;

		private JdbcParametersListSingleton(JdbcParameter singleElement) {
			this.singleElement = singleElement;
		}

		public JdbcParameter get(int selectionIndex) {
			if ( selectionIndex != 0 ) {
				throw new ArrayIndexOutOfBoundsException( selectionIndex );
			}
			return singleElement;
		}

		public int size() {
			return 1;
		}
	}

}
