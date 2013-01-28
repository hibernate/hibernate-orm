/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.dialect.unique;

import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.Table;
import org.hibernate.metamodel.relational.UniqueKey;

/**
 * Dialect-level delegate in charge of applying "uniqueness" to a column.
 * Uniqueness can be defined in 1 of 3 ways:
 * 
 * 1.) Add a unique constraint via separate alter table statements.
 * 2.) Add a unique constraint via dialect-specific syntax in table create statement.
 * 3.) Add "unique" syntax to the column itself.
 * 
 * #1 & #2 are preferred, if possible -- #3 should be solely a fall-back.
 * 
 * TODO: This could eventually be simplified.  With AST, 1 "applyUniqueness"
 * method might be possible. But due to .cfg and .mapping still resolving
 * around StringBuilders, separate methods were needed.
 * 
 * See HHH-7797.
 * 
 * @author Brett Meyer
 */
public interface UniqueDelegate {
	
	/**
	 * If the dialect does not supports unique constraints, this method should
	 * return the syntax necessary to mutate the column definition
	 * (usually "unique").
	 * 
	 * @param column
	 * @return String
	 */
	public String applyUniqueToColumn( org.hibernate.mapping.Column column );
	
	/**
	 * If the dialect does not supports unique constraints, this method should
	 * return the syntax necessary to mutate the column definition
	 * (usually "unique").
	 * 
	 * @param column
	 * @return String
	 */
	public String applyUniqueToColumn( Column column );
	
	/**
	 * If constraints are supported, but not in seperate alter statements,
	 * return uniqueConstraintSql in order to add the constraint to the
	 * original table definition.
	 * 
	 * @param table
	 * @return String
	 */
	public String applyUniquesToTable( org.hibernate.mapping.Table table );
	
	/**
	 * If constraints are supported, but not in seperate alter statements,
	 * return uniqueConstraintSql in order to add the constraint to the
	 * original table definition.
	 * 
	 * @param table
	 * @return String
	 */
	public String applyUniquesToTable( Table table );
	
	/**
	 * If creating unique constraints in separate alter statements is
	 * supported, generate the necessary "alter" syntax for the given key.
	 * 
	 * @param uniqueKey
	 * @param defaultCatalog
	 * @param defaultSchema
	 * @return String
	 */
	public String applyUniquesOnAlter( org.hibernate.mapping.UniqueKey uniqueKey,
			String defaultCatalog, String defaultSchema );
	
	/**
	 * If creating unique constraints in separate alter statements is
	 * supported, generate the necessary "alter" syntax for the given key.
	 * 
	 * @param uniqueKey
	 * @return String
	 */
	public String applyUniquesOnAlter( UniqueKey uniqueKey );
	
	/**
	 * If dropping unique constraints in separate alter statements is
	 * supported, generate the necessary "alter" syntax for the given key.
	 * 
	 * @param uniqueKey
	 * @param defaultCatalog
	 * @param defaultSchema
	 * @return String
	 */
	public String dropUniquesOnAlter( org.hibernate.mapping.UniqueKey uniqueKey,
			String defaultCatalog, String defaultSchema );
	
	/**
	 * If dropping unique constraints in separate alter statements is
	 * supported, generate the necessary "alter" syntax for the given key.
	 * 
	 * @param uniqueKey
	 * @return String
	 */
	public String dropUniquesOnAlter( UniqueKey uniqueKey );
	
	/**
	 * Generates the syntax necessary to create the unique constraint (reused
	 * by all methods).  Ex: "unique (column1, column2, ...)"
	 * 
	 * @param uniqueKey
	 * @return String
	 */
	public String uniqueConstraintSql( org.hibernate.mapping.UniqueKey uniqueKey );
	
	/**
	 * Generates the syntax necessary to create the unique constraint (reused
	 * by all methods).  Ex: "unique (column1, column2, ...)"
	 * 
	 * @param uniqueKey
	 * @return String
	 */
	public String uniqueConstraintSql( UniqueKey uniqueKey );
}
