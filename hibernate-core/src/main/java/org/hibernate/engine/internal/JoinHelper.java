/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.internal;

import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.type.AssociationType;

/**
 * @author Gavin King
 */
public final class JoinHelper {
	
	private JoinHelper() {}
	
	/**
	 * Get the aliased columns of the owning entity which are to 
	 * be used in the join
	 */
	public static String[] getAliasedLHSColumnNames(
			AssociationType type, 
			String alias, 
			int property, 
			OuterJoinLoadable lhsPersister,
			Mapping mapping
	) {
		return getAliasedLHSColumnNames(type, alias, property, 0, lhsPersister, mapping);
	}
	
	/**
	 * Get the columns of the owning entity which are to 
	 * be used in the join
	 */
	public static String[] getLHSColumnNames(
			AssociationType type, 
			int property, 
			OuterJoinLoadable lhsPersister,
			Mapping mapping
	) {
		return getLHSColumnNames(type, property, 0, lhsPersister, mapping);
	}
	
	/**
	 * Get the aliased columns of the owning entity which are to 
	 * be used in the join
	 */
	public static String[] getAliasedLHSColumnNames(
			AssociationType associationType,
			String columnQualifier,
			int propertyIndex,
			int begin, 
			OuterJoinLoadable lhsPersister,
			Mapping mapping) {
		if ( associationType.useLHSPrimaryKey() ) {
			return StringHelper.qualify( columnQualifier, lhsPersister.getIdentifierColumnNames() );
		}
		else {
			String propertyName = associationType.getLHSPropertyName();
			if ( propertyName == null ) {
				return ArrayHelper.slice(
						toColumns( lhsPersister, columnQualifier, propertyIndex ),
						begin,
						associationType.getColumnSpan( mapping )
				);
			}
			else {
				return ( (PropertyMapping) lhsPersister ).toColumns(columnQualifier, propertyName); //bad cast
			}
		}
	}

	private static String[] toColumns(OuterJoinLoadable persister, String columnQualifier, int propertyIndex) {
		if ( propertyIndex >= 0 ) {
			return persister.toColumns( columnQualifier, propertyIndex );
		}
		else {
			final String[] cols = persister.getIdentifierColumnNames();
			final String[] result = new String[cols.length];

			for ( int j = 0; j < cols.length; j++ ) {
				result[j] = StringHelper.qualify( columnQualifier, cols[j] );
			}

			return result;
		}
	}

	/**
	 * Get the columns of the owning entity which are to 
	 * be used in the join
	 */
	public static String[] getLHSColumnNames(
			AssociationType type, 
			int property, 
			int begin, 
			OuterJoinLoadable lhsPersister,
			Mapping mapping
	) {
		if ( type.useLHSPrimaryKey() ) {
			//return lhsPersister.getSubclassPropertyColumnNames(property);
			return lhsPersister.getIdentifierColumnNames();
		}
		else {
			String propertyName = type.getLHSPropertyName();
			if (propertyName==null) {
				//slice, to get the columns for this component
				//property
				return ArrayHelper.slice(
						property < 0
								? lhsPersister.getIdentifierColumnNames()
								: lhsPersister.getSubclassPropertyColumnNames(property),
						begin, 
						type.getColumnSpan(mapping) 
					);
			}
			else {
				//property-refs for associations defined on a
				//component are not supported, so no need to slice
				return lhsPersister.getPropertyColumnNames(propertyName);
			}
		}
	}
	
	public static String getLHSTableName(
			AssociationType type,
			int propertyIndex,
			OuterJoinLoadable lhsPersister) {
		if ( type.useLHSPrimaryKey() || propertyIndex < 0 ) {
			return lhsPersister.getTableName();
		}
		else {
			String propertyName = type.getLHSPropertyName();
			if (propertyName==null) {
				//if there is no property-ref, assume the join
				//is to the subclass table (ie. the table of the
				//subclass that the association belongs to)
				return lhsPersister.getSubclassPropertyTableName(propertyIndex);
			}
			else {
				//handle a property-ref
				String propertyRefTable = lhsPersister.getPropertyTableName(propertyName);
				if (propertyRefTable==null) {
					//it is possible that the tree-walking in OuterJoinLoader can get to
					//an association defined by a subclass, in which case the property-ref
					//might refer to a property defined on a subclass of the current class
					//in this case, the table name is not known - this temporary solution 
					//assumes that the property-ref refers to a property of the subclass
					//table that the association belongs to (a reasonable guess)
					//TODO: fix this, add: OuterJoinLoadable.getSubclassPropertyTableName(String propertyName)
					propertyRefTable = lhsPersister.getSubclassPropertyTableName(propertyIndex);
				}
				return propertyRefTable;
			}
		}
	}
	
	/**
	 * Get the columns of the associated table which are to 
	 * be used in the join
	 */
	public static String[] getRHSColumnNames(AssociationType type, SessionFactoryImplementor factory) {
		String uniqueKeyPropertyName = type.getRHSUniqueKeyPropertyName();
		Joinable joinable = type.getAssociatedJoinable(factory);
		if (uniqueKeyPropertyName==null) {
			return joinable.getKeyColumnNames();
		}
		else {
			return ( (OuterJoinLoadable) joinable ).getPropertyColumnNames(uniqueKeyPropertyName);
		}
	}
}
