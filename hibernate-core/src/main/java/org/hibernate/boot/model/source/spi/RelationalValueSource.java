/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Unifying interface for {@link ColumnSource} and {@link DerivedValueSource}.
 *
 * @author Steve Ebersole
 *
 * @see ColumnSource
 * @see DerivedValueSource
 */
public interface RelationalValueSource {
	/**
	 * @return returns the name of the table that contains this value.
	 */
	public String getContainingTableName();

	/**
	 * Retrieve the nature of this relational value.  Is it a column?  Or is it a derived value (formula)?
	 *
	 * @return The nature.
	 */
	public Nature getNature();

	public static enum Nature {
		COLUMN( ColumnSource.class ),
		DERIVED( DerivedValueSource.class );

		private final Class<? extends RelationalValueSource> specificContractClass;

		private Nature(Class<? extends RelationalValueSource> specificContractClass) {
			this.specificContractClass = specificContractClass;
		}

		public Class<? extends RelationalValueSource> getSpecificContractClass() {
			return specificContractClass;
		}
	}
}
