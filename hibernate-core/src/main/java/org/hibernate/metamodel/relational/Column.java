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
	private boolean nullable;
	private boolean unique;

	private String defaultValue;
	private String checkCondition;
	private String sqlType;

	private String readFragment;
	private String writeFragment;

	private String comment;

	private Size size = new Size();

	protected Column(TableSpecification table, int position, String name) {
		super( table, position );
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public boolean isNullable() {
		return nullable;
	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}

	public boolean isUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getCheckCondition() {
		return checkCondition;
	}

	public void setCheckCondition(String checkCondition) {
		this.checkCondition = checkCondition;
	}

	public String getSqlType() {
		return sqlType;
	}

	public void setSqlType(String sqlType) {
		this.sqlType = sqlType;
	}

	public String getReadFragment() {
		return readFragment;
	}

	public void setReadFragment(String readFragment) {
		this.readFragment = readFragment;
	}

	public String getWriteFragment() {
		return writeFragment;
	}

	public void setWriteFragment(String writeFragment) {
		this.writeFragment = writeFragment;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Size getSize() {
		return size;
	}

	public void setSize(Size size) {
		this.size = size;
	}

	@Override
	public String toLoggableString() {
		return getTable().getLoggableValueQualifier() + '.' + getName();
	}

	/**
	 * Models size restrictions/requirements on a column's datatype.
	 * <p/>
	 * IMPL NOTE: since we do not necessarily know the datatype up front, and therefore do not necessarily know
	 * whether length or precision/scale sizing is needed, we simply account for both here.  Additionally LOB
	 * definitions, by standard, are allowed a "multiplier" consisting of 'K' (Kb), 'M' (Mb) or 'G' (Gb).
	 */
	public static class Size {
		private static enum LobMultiplier {
			NONE( 1 ),
			K( NONE.factor * 1024 ),
			M( K.factor * 1024 ),
			G( M.factor * 1024 );

			private long factor;

			private LobMultiplier(long factor) {
				this.factor = factor;
			}

			public long getFactor() {
				return factor;
			}
		}

		private int precision = - 1;
		private int scale = -1;
		private long length = -1;
		private LobMultiplier lobMultiplier = LobMultiplier.NONE;

		public Size() {
		}

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

		public static Size precision(int precision) {
			return new Size( precision, -1, -1, null );
		}

		public static Size precision(int precision, int scale) {
			return new Size( precision, scale, -1, null );
		}

		public static Size length(long length) {
			return new Size( -1, -1, length, null );
		}

		public static Size length(long length, LobMultiplier lobMultiplier) {
			return new Size( -1, -1, length, lobMultiplier );
		}

		public int getPrecision() {
			return precision;
		}

		public int getScale() {
			return scale;
		}

		public long getLength() {
			return length;
		}

		public LobMultiplier getLobMultiplier() {
			return lobMultiplier;
		}

		public void setPrecision(int precision) {
			this.precision = precision;
		}

		public void setScale(int scale) {
			this.scale = scale;
		}

		public void setLength(long length) {
			this.length = length;
		}

		public void setLobMultiplier(LobMultiplier lobMultiplier) {
			this.lobMultiplier = lobMultiplier;
		}
	}
}
