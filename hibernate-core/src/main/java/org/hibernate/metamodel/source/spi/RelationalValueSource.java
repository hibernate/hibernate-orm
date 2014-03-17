/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.source.spi;

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
