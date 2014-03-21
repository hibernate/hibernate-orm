/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.dialect.constraint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.UniqueConstraint;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.spi.relational.AbstractConstraint;
import org.hibernate.metamodel.spi.relational.Index;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.metamodel.spi.relational.UniqueKey;
import org.jboss.logging.Logger;

/**
 * Databases differ in the ways they handle indexes and unique constraints.  Due to the somewhat complex rulesets,
 * the constraints export is tied to the Dialect through this delegate.  Through the list of {@link Table}s,
 * we're able to handle permutations of:
 * <ul>
 * <li>unique columns (ex: {@link Column#unique()})</li>
 * <li>{@link UniqueConstraint}</li>
 * <li>{@link javax.persistence.Index} (including {@link javax.persistence.Index#unique()}</li>
 * </ul>
 * 
 * Each Dialect then determines the correct set of constraints to export based on the entire bucket of index and
 * uniqueness concepts.  This implementation attempts to cover *most* cases.  However, if you require something specific, it can be easily
 * extended and provided through a custom {@link Dialect}.
 * 
 * Note that we introduce another layer of duplicate prevention (indexColumnListIds & uniqueColumnListIds).
 * Some duplication is already prevented further up (ex: through {@link Index#getExportIdentifier()}), but that primarily
 * prevents duplication caused by explicit mappings.
 * 
 * @author Brett Meyer
 */
public class ConstraintDelegate {

	private static final Logger LOG = Logger.getLogger( ConstraintDelegate.class );
	
	final protected Dialect dialect;
	
	public ConstraintDelegate(Dialect dialect) {
		this.dialect = dialect;
	}
	
	/**
	 * Generate the list of constraint-creating SQL strings from the given set of {@link Table}s.
	 * 
	 * @param tables
	 * @param jdbcEnvironment
	 * @return String[] The list of constraint-creating SQL strings.
	 */
	public String[] applyConstraints(Iterable<Table> tables, JdbcEnvironment jdbcEnvironment) {
		final List<String> sqlStrings = new ArrayList<String>();
		// List of "columnListIds" (hashes of column names), FIFO, used to prevent duplicate index creation.
		final List<Integer> indexColumnListIds = new ArrayList<Integer>();
		// List of "columnListIds" (hashes of column names), alphabetical, used to prevent duplicate unique constraint creation.
		final List<Integer> uniqueColumnListIds = new ArrayList<Integer>();
		
		for ( Table table : tables ) {
			if( !table.isPhysicalTable() ){
				continue;
			}
			
			for ( Index index : table.getIndexes() ) {
				createIndex( index, sqlStrings, indexColumnListIds, uniqueColumnListIds, jdbcEnvironment );
			}

			for  ( UniqueKey uniqueKey : table.getUniqueKeys() ) {
				createUnique( uniqueKey, sqlStrings, indexColumnListIds, uniqueColumnListIds, jdbcEnvironment );
			}
		}
		
		return sqlStrings.toArray( new String[sqlStrings.size()] );
	}
	
	/**
	 * Create the SQL string for the given {@link Index}.  By default, this creates a unique constraint if
	 * {@link Index#isUnique()} returns true.  Otherwise, create the index.
	 * 
	 * Note that an index for (col1, col2) is not the same as (col2, col1), so both should be created.  However,
	 * only one single unique constraint should be created for (col1, col2) if {@link Index#isUnique()} returns true.
	 * 
	 * @param index
	 * @param sqlStrings
	 * @param indexColumnListIds
	 * @param uniqueColumnListIds
	 * @param jdbcEnvironment
	 */
	protected void createIndex(Index index, List<String> sqlStrings, List<Integer> indexColumnListIds,
			List<Integer> uniqueColumnListIds, JdbcEnvironment jdbcEnvironment) {
		if (index.isUnique()) {
			createUnique( index, sqlStrings, indexColumnListIds, uniqueColumnListIds, jdbcEnvironment );
		}
		else {
			if (indexColumnListIds.contains( index.columnListId() )) {
				LOG.warnf( "The mapping would have resulted in the creation of duplicate indexes for the following"
						+ " sequence of columns: %s", index.getColumnNames());
			}
			else {
				sqlStrings.addAll(Arrays.asList( dialect.getIndexExporter().getSqlCreateStrings( index, jdbcEnvironment ) ) );
			}
			indexColumnListIds.add( index.columnListId() );
		}
	}
	
	/**
	 * Create the unique constraint SQL string for the given {@link UniqueKey} or unique {@link Index}.
	 * 
	 * @param constraint
	 * @param sqlStrings
	 * @param indexColumnListIds
	 * @param uniqueColumnListIds
	 * @param jdbcEnvironment
	 */
	protected void createUnique(AbstractConstraint constraint, List<String> sqlStrings,
			List<Integer> indexColumnListIds, List<Integer> uniqueColumnListIds, JdbcEnvironment jdbcEnvironment) {
		// A unique Index may have already exported the unique constraint.
		if (uniqueColumnListIds.contains( constraint.columnListAlphabeticalId() )) {
			LOG.warnf( "The mapping would have resulted in the creation of duplicate unique constraints for the"
					+ " following sequence of columns.  Note that Hibernate automatically creates a unique constraint"
					+ " for a unique index.  %s", constraint.getColumnNames());
		}
		else {
			sqlStrings.addAll(Arrays.asList( dialect.getUniqueKeyExporter().getSqlCreateStrings(
					constraint, jdbcEnvironment ) ) );
		}
		uniqueColumnListIds.add( constraint.columnListAlphabeticalId() );
	}
	
	/**
	 * Generate the list of constraint-dropping SQL strings from the given set of {@link Table}s, if supported
	 * by {@link Dialect#dropConstraints()}.
	 * 
	 * @param tables
	 * @param jdbcEnvironment
	 * @return String[] The list of constraint-dropping SQL strings.
	 */
	public String[] dropConstraints(Iterable<Table> tables, JdbcEnvironment jdbcEnvironment) {
		final List<String> sqlStrings = new ArrayList<String>();
		// List of "columnListIds" (hashes of column names), FIFO, used to prevent duplicate index creation.
		final List<Integer> indexColumnListIds = new ArrayList<Integer>();
		// List of "columnListIds" (hashes of column names), alphabetical, used to prevent duplicate unique constraint creation.
		final List<Integer> uniqueColumnListIds = new ArrayList<Integer>();
		
		if (dialect.dropConstraints()) {
			for ( Table table : tables ) {
				if( !table.isPhysicalTable() ){
					continue;
				}
				
				for ( Index index : table.getIndexes() ) {
					dropIndex( index, sqlStrings, indexColumnListIds, uniqueColumnListIds, jdbcEnvironment );
				}
	
				for  ( UniqueKey uniqueKey : table.getUniqueKeys() ) {
					dropUnique( uniqueKey, sqlStrings, indexColumnListIds, uniqueColumnListIds, jdbcEnvironment );
				}
			}
		}
		
		return sqlStrings.toArray( new String[sqlStrings.size()] );
	}
	
	/**
	 * See {@link #createIndex(Index, List, List, JdbcEnvironment)}.  The concepts are the same, except
	 * this targets dropping.
	 * 
	 * @param index
	 * @param sqlStrings
	 * @param indexColumnListIds
	 * @param uniqueColumnListIds
	 * @param jdbcEnvironment
	 */
	protected void dropIndex(Index index, List<String> sqlStrings, List<Integer> indexColumnListIds,
			List<Integer> uniqueColumnListIds, JdbcEnvironment jdbcEnvironment) {
		if (index.isUnique()) {
			dropUnique( index, sqlStrings, indexColumnListIds, uniqueColumnListIds, jdbcEnvironment );
		}
		else {
			if (! indexColumnListIds.contains( index.columnListId() )) {
				sqlStrings.addAll(Arrays.asList( dialect.getIndexExporter().getSqlDropStrings( index, jdbcEnvironment ) ) );
			}
			indexColumnListIds.add( index.columnListId() );
		}
	}
	
	/**
	 * See {@link #createUnique(AbstractConstraint, List, List, JdbcEnvironment)}.  The concepts are the same, except
	 * this targets dropping.
	 * 
	 * @param constraint
	 * @param sqlStrings
	 * @param indexColumnListIds
	 * @param uniqueColumnListIds
	 * @param jdbcEnvironment
	 */
	protected void dropUnique(AbstractConstraint constraint, List<String> sqlStrings, List<Integer> indexColumnListIds,
			List<Integer> uniqueColumnListIds, JdbcEnvironment jdbcEnvironment) {
		// A unique Index may have already exported the constraint.
		if (! uniqueColumnListIds.contains( constraint.columnListAlphabeticalId() )) {
			sqlStrings.addAll(Arrays.asList( dialect.getUniqueKeyExporter().getSqlDropStrings(
					constraint, jdbcEnvironment ) ) );
		}
		uniqueColumnListIds.add( constraint.columnListAlphabeticalId() );
	}
}
