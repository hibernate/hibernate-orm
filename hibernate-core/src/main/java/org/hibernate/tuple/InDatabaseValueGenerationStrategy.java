/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.tuple;

/**
 * Strategy for describing values which are generated in the database.
 *
 * @author Steve Ebersole
 */
public interface InDatabaseValueGenerationStrategy {
	/**
	 * When is this value generated : NEVER, INSERT, ALWAYS (INSERT+UPDATE)
	 *
	 * @return When the value is generated.
	 */
	public GenerationTiming getGenerationTiming();

	/**
	 * Should the column(s) be referenced in the INSERT / UPDATE SQL?
	 * <p/>
	 * This will be {@code false} most often to have a DDL-defined DEFAULT value be applied on INSERT.  For
	 * trigger-generated values this could be {@code true} or {@code false} depending on whether the user wants
	 * the trigger to have access to some value for the column passed in.
	 *
	 * @return {@code true} indicates the column should be included in the SQL.
	 */
	public boolean referenceColumnsInSql();

	/**
	 * For columns that will be referenced in the SQL (per {@link #referenceColumnsInSql()}), what value
	 * should be used in the SQL as the column value.
	 *
	 * @return The column value to be used in the SQL.  {@code null} for any element indicates to use the Column
	 * defined value ({@link org.hibernate.mapping.Column#getWriteExpr}).
	 */
	public String[] getReferencedColumnValues();

}
