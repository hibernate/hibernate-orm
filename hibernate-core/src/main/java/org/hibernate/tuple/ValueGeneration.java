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
