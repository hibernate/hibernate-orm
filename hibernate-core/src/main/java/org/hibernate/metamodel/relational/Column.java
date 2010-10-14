/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.relational;

/**
 * Models a physical column
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class Column extends AbstractSimpleValue implements SimpleValue {
	private final String name;
	private Size size;

	protected Column(ValueContainer table, String name) {
		super( table );
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Size getSize() {
		return size;
	}

	public void setSize(Size size) {
		this.size = size;
	}

	@Override
	public String toLoggableString() {
		return getValueContainer().getLoggableValueQualifier() + '.' + getName();
	}

	/**
	 * Models size restrictions/requirements on a column's datatype.
	 * <p/>
	 * IMPL NOTE: since we do not necessarily know the datatype up front, and therefore do not necessarily know
	 * whether length or precision/scale sizing is needed, we simply account for both here.  Additionally LOB
	 * definitions, by standard, are allowed a "multiplier" consisting of 'K' (Kb), 'M' (Mb) or 'G' (Gb).
	 */
	public static class Size {
		private static enum LobMultiplier { K, M, G }

		private final int precision;
		private final  int scale;
		private final long length;
		private final LobMultiplier lobMultiplier;

		/**
		 * Complete constructor.
		 *
		 * @param precision numeric precision
		 * @param scale numeric scale
		 * @param length type length
		 * @param lobMultiplier LOB length multiplier
		 */
		public Size(int precision, int scale, long length, LobMultiplier lobMultiplier) {
			this.precision = precision;
			this.scale = scale;
			this.length = length;
			this.lobMultiplier = lobMultiplier;
		}

		public Size(int precision) {
			this( precision, -1, -1, null );
		}

		public Size(int precision, int scale) {
			this( precision, scale, -1, null );
		}

		public Size(long length) {
			this( -1, -1, length, null );
		}

		public Size(long length, LobMultiplier lobMultiplier) {
			this( -1, -1, length, lobMultiplier );
		}
	}
}
