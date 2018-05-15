/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import java.io.Serializable;

import org.hibernate.HibernateException;

/**
 * Models size restrictions/requirements on a column's datatype.
 * <p/>
 * IMPL NOTE: since we do not necessarily know the datatype up front, and therefore do not necessarily know
 * whether length or precision/scale sizing is needed, we simply account for both here.  Additionally LOB
 * definitions, by standard, are allowed a "multiplier" consisting of 'K' (Kb), 'M' (Mb) or 'G' (Gb).
 *
 * @author Steve Ebersole
 */
public class Size implements Serializable {
	private final Integer precision;
	private final Integer scale;

	private final Long length;
	private final LobMultiplier lobMultiplier;


	/**
	 * Complete constructor.
	 *
	 * @param precision numeric precision
	 * @param scale numeric scale
	 * @param length type length
	 * @param lobMultiplier LOB length multiplier
	 */
	private Size(Integer precision, Integer scale, Long length, LobMultiplier lobMultiplier) {
		this.precision = precision;
		this.scale = scale;
		this.length = length;
		this.lobMultiplier = lobMultiplier;
	}

	public Integer getPrecision() {
		return precision;
	}

	public Integer getScale() {
		return scale;
	}

	public Long getLength() {
		return length;
	}

	public LobMultiplier getLobMultiplier() {
		return lobMultiplier;
	}


	public enum LobMultiplier {
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


	public static class Builder {
		// todo (6.0) : account for legacy size defaults.
		// 		these are the legacy size defaults Hibernate applied indiscriminately.
		//		moving forward we have a better solution for this with `DefaultSizeStrategy`,
		//		but we do need to allow for user's to enable legacy behavior (aka these values)
		//		if they wish - probably a named `DefaultSizeStrategy` instance

		public static final long DEFAULT_LENGTH = 255;
		public static final int DEFAULT_PRECISION = 19;
		public static final int DEFAULT_SCALE = 2;


		public static Size length(long length) {
			return new Size( null, null, length, null );
		}

		public static Size length(long length, LobMultiplier lobMultiplier) {
			return new Size( null, null, length, lobMultiplier );
		}

		public static Size precision(int precision) {
			return new Size( precision, null, null, null );
		}

		public static Size precision(int precision, int scale) {
			return new Size( precision, scale, null, null );
		}

		private Integer precision;
		private Integer scale;

		private Long length;
		private LobMultiplier lobMultiplier;

		public Builder() {
			lobMultiplier = LobMultiplier.NONE;
		}


		public Long getLength() {
			return length;
		}

		public Builder setLength(Long length) {
			this.length = length;
			return this;
		}

		public LobMultiplier getLobMultiplier() {
			return lobMultiplier;
		}

		public Builder setLobMultiplier(LobMultiplier lobMultiplier) {
			this.lobMultiplier = lobMultiplier;
			return this;
		}

		public Integer getPrecision() {
			return precision;
		}

		public Builder setPrecision(Integer precision) {
			this.precision = precision;
			return this;
		}

		public Integer getScale() {
			return scale;
		}

		public Builder setScale(Integer scale) {
			this.scale = scale;
			return this;
		}

		public Size build() {
			if ( length != null && ( precision != null || scale != null ) ) {
				throw new HibernateException( "Illegal attempt to specify both length and precision/scale for column size" );
			}

			if ( precision != null && lobMultiplier != LobMultiplier.NONE ) {
				throw new HibernateException( "Illegal attempt to specify LobMultiplier with precision/scale" );
			}

			return new Size( precision, scale, length, lobMultiplier );
		}
	}
}
