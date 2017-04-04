/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
