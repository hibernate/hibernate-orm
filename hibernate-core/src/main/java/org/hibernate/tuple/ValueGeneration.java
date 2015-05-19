/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

/**
 * Describes the generation of property values.
 *
 * @author Steve Ebersole
 */
public interface ValueGeneration {
	/**
	 * When is this value generated : NEVER, INSERT, ALWAYS (INSERT+UPDATE)
	 *
	 * @return When the value is generated.
	 */
	public GenerationTiming getGenerationTiming();

	/**
	 * Obtain the in-VM value generator.
	 * <p/>
	 * May return {@code null}.  In fact for values that are generated "in the database" via execution of the
	 * INSERT/UPDATE statement, the expectation is that {@code null} be returned here
	 *
	 * @return The strategy for performing in-VM value generation
	 */
	public ValueGenerator<?> getValueGenerator();

	/**
	 * For values which are generated in the database ({@link #getValueGenerator()} == {@code null}), should the
	 * column be referenced in the INSERT / UPDATE SQL?
	 * <p/>
	 * This will be false most often to have a DDL-defined DEFAULT value be applied on INSERT
	 *
	 * @return {@code true} indicates the column should be included in the SQL.
	 */
	public boolean referenceColumnInSql();

	/**
	 * For values which are generated in the database ({@link #getValueGenerator} == {@code null}), if the
	 * column will be referenced in the SQL ({@link #referenceColumnInSql()} == {@code true}), what value should be
	 * used in the SQL as the column value.
	 * <p/>
	 * Generally this will be a function call or a marker (DEFAULTS).
	 * <p/>
	 * NOTE : for in-VM generation, this will not be called and the column value will implicitly be a JDBC parameter ('?')
	 *
	 * @return The column value to be used in the SQL.
	 */
	public String getDatabaseGeneratedReferencedColumnValue();
}
