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
package org.hibernate.metamodel.spi.relational;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;

/**
 * Support for writing {@link Constraint} implementations
 *
 * @todo do we need to support defining these on particular schemas/catalogs?
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public abstract class AbstractConstraint implements Constraint {
	private TableSpecification table;
	private String name;
	private final Map<Identifier, Column> columnMap = new LinkedHashMap<Identifier, Column>();

	protected AbstractConstraint(TableSpecification table, String name) {
		this.table = table;
		this.name = name;
	}

	@Override
	public TableSpecification getTable() {
		return table;
	}
	
	public void setTable( TableSpecification table ) {
		this.table = table;
	}

	/**
	 * Returns the constraint name, or null if the name has not been set.
	 *
	 * @return the constraint name, or null if the name has not been set
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets a constraint name that is unique across
	 * all database objects.
	 *
	 * @param name - the unique constraint name; must be non-null.
	 *
	 * @throws IllegalArgumentException if name is null.
	 * @throws IllegalStateException if this constraint already has a non-null name.
	 */
	public void setName(String name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "name must be non-null." );
		}
		if ( this.name != null ) {
			throw new IllegalStateException(
					String.format(
							"This constraint already has a name (%s) and cannot be renamed to (%s).",
							this.name,
							name
					)
			);
		}
		this.name = name;
	}

	/**
	 * Hash a constraint name using MD5. Convert the MD5 digest to base 35
	 * (full alphanumeric), guaranteeing
	 * that the length of the name will always be smaller than the 30
	 * character identifier restriction enforced by a few dialects.
	 * 
	 * @param s
	 *            The name to be hashed.
	 * @return String The hased name.
	 */
	public static String hashedName(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance( "MD5" );
			md.reset();
			md.update( s.getBytes() );
			byte[] digest = md.digest();
			BigInteger bigInt = new BigInteger( 1, digest );
			// By converting to base 35 (full alphanumeric), we guarantee
			// that the length of the name will always be smaller than the 30
			// character identifier restriction enforced by a few dialects.
			return bigInt.toString( 35 );
		}
		catch ( NoSuchAlgorithmException e ) {
			throw new HibernateException( "Unable to generate a hashed Constraint name!", e );
		}
	}

	protected int generateConstraintColumnListId() {
		return table.generateColumnListId( getColumns() );
	}

	public List<Column> getColumns() {
		return Collections.unmodifiableList( new ArrayList<Column>( columnMap.values() ) );
	}

	public int getColumnSpan() {
		return columnMap.size();
	}
	
	public boolean hasColumn(Column column) {
		return columnMap.containsKey( column.getColumnName() );
	}
	
	public boolean hasColumn(String columnName) {
		for ( Identifier key : columnMap.keySet() ) {
			if ( key.getText().equals( columnName ) ) return true;
		}
		return false;
	}

	protected Map<Identifier, Column> internalColumnAccess() {
		return columnMap;
	}

	public void addColumn(Column column) {
		internalAddColumn( column );
	}

	protected void internalAddColumn(Column column) {
//		if ( column.getTable() != getTable() ) {
//			throw new AssertionFailure(
//					String.format(
//							"Unable to add column to constraint; tables [%s, %s] did not match",
//							column.getTable().toLoggableString(),
//							getTable().toLoggableString()
//					)
//			);
//		}
		columnMap.put( column.getColumnName(), column );
	}

	protected boolean isCreationVetoed(Dialect dialect) {
		return false;
	}

	protected abstract String sqlConstraintStringInAlterTable(Dialect dialect);

	public String[] sqlDropStrings(Dialect dialect) {
		if ( isCreationVetoed( dialect ) ) {
			return null;
		}
		else {
			return new String[] {
					new StringBuilder()
						.append( "alter table " )
						.append( getTable().getQualifiedName( dialect ) )
						.append( " drop constraint " )
						.append( dialect.quote( name ) )
						.toString()
			};
		}
	}

	public String[] sqlCreateStrings(Dialect dialect) {
		if ( isCreationVetoed( dialect ) ) {
			return null;
		}
		else {
			return new String[] {
					new StringBuilder( "alter table " )
							.append( getTable().getQualifiedName( dialect ) )
							.append( sqlConstraintStringInAlterTable( dialect ) )
							.toString()
			};
		}
	}
}
