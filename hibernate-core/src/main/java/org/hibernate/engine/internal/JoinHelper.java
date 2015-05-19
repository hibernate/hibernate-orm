/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 * Helper for dealing with joins.
 *
 * @author Gavin King
 */
public final class JoinHelper {
	/**
	 * Get the qualified (prefixed by alias) names of the columns of the owning entity which are to be used in the join
	 *
	 * @param type The association type for the association that represents the join
	 * @param alias The left-hand side table alias
	 * @param property The index of the property that represents the association/join
	 * @param lhsPersister The persister for the left-hand side of the association/join
	 * @param mapping The mapping (typically the SessionFactory).
	 *
	 * @return The qualified column names.
	 */
	public static String[] getAliasedLHSColumnNames(
			AssociationType type,
			String alias,
			int property,
			OuterJoinLoadable lhsPersister,
			Mapping mapping) {
		return getAliasedLHSColumnNames( type, alias, property, 0, lhsPersister, mapping );
	}

	/**
	 * Get the unqualified names of the columns of the owning entity which are to be used in the join.
	 *
	 * @param type The association type for the association that represents the join
	 * @param property The name of the property that represents the association/join
	 * @param lhsPersister The persister for the left-hand side of the association/join
	 * @param mapping The mapping (typically the SessionFactory).
	 *
	 * @return The unqualified column names.
	 */
	public static String[] getLHSColumnNames(
			AssociationType type,
			int property,
			OuterJoinLoadable lhsPersister,
			Mapping mapping) {
		return getLHSColumnNames( type, property, 0, lhsPersister, mapping );
	}

	/**
	 *
	 */
	/**
	 * Get the qualified (prefixed by alias) names of the columns of the owning entity which are to be used in the join
	 *
	 * @param associationType The association type for the association that represents the join
	 * @param columnQualifier The left-hand side table alias
	 * @param propertyIndex The index of the property that represents the association/join
	 * @param begin The index for any nested (composites) attributes
	 * @param lhsPersister The persister for the left-hand side of the association/join
	 * @param mapping The mapping (typically the SessionFactory).
	 *
	 * @return The qualified column names.
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
			final String propertyName = associationType.getLHSPropertyName();
			if ( propertyName == null ) {
				return ArrayHelper.slice(
						toColumns( lhsPersister, columnQualifier, propertyIndex ),
						begin,
						associationType.getColumnSpan( mapping )
				);
			}
			else {
				//bad cast
				return ( (PropertyMapping) lhsPersister ).toColumns( columnQualifier, propertyName );
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
	 * Get the columns of the owning entity which are to be used in the join
	 *
	 * @param type The type representing the join
	 * @param property The property index for the association type
	 * @param begin ?
	 * @param lhsPersister The persister for the left-hand-side of the join
	 * @param mapping The mapping object (typically the SessionFactory)
	 *
	 * @return The columns for the left-hand-side of the join
	 */
	public static String[] getLHSColumnNames(
			AssociationType type,
			int property,
			int begin,
			OuterJoinLoadable lhsPersister,
			Mapping mapping) {
		if ( type.useLHSPrimaryKey() ) {
			//return lhsPersister.getSubclassPropertyColumnNames(property);
			return lhsPersister.getIdentifierColumnNames();
		}
		else {
			final String propertyName = type.getLHSPropertyName();
			if ( propertyName == null ) {
				//slice, to get the columns for this component
				//property
				return ArrayHelper.slice(
						property < 0
								? lhsPersister.getIdentifierColumnNames()
								: lhsPersister.getSubclassPropertyColumnNames( property ),
						begin,
						type.getColumnSpan( mapping )
				);
			}
			else {
				//property-refs for associations defined on a
				//component are not supported, so no need to slice
				return lhsPersister.getPropertyColumnNames( propertyName );
			}
		}
	}

	/**
	 * Determine the name of the table that is the left-hand-side of the join.  Usually this is the
	 * name of the main table from the left-hand-side persister.  But that is not the case with property-refs.
	 *
	 * @param type The type representing the join
	 * @param propertyIndex The property index for the type
	 * @param lhsPersister The persister for the left-hand-side of the join
	 *
	 * @return The table name
	 */
	public static String getLHSTableName(
			AssociationType type,
			int propertyIndex,
			OuterJoinLoadable lhsPersister) {
		if ( type.useLHSPrimaryKey() || propertyIndex < 0 ) {
			return lhsPersister.getTableName();
		}
		else {
			final String propertyName = type.getLHSPropertyName();
			if ( propertyName == null ) {
				//if there is no property-ref, assume the join
				//is to the subclass table (ie. the table of the
				//subclass that the association belongs to)
				return lhsPersister.getSubclassPropertyTableName( propertyIndex );
			}
			else {
				//handle a property-ref
				String propertyRefTable = lhsPersister.getPropertyTableName( propertyName );
				if ( propertyRefTable == null ) {
					//it is possible that the tree-walking in OuterJoinLoader can get to
					//an association defined by a subclass, in which case the property-ref
					//might refer to a property defined on a subclass of the current class
					//in this case, the table name is not known - this temporary solution 
					//assumes that the property-ref refers to a property of the subclass
					//table that the association belongs to (a reasonable guess)
					//TODO: fix this, add: OuterJoinLoadable.getSubclassPropertyTableName(String propertyName)
					propertyRefTable = lhsPersister.getSubclassPropertyTableName( propertyIndex );
				}
				return propertyRefTable;
			}
		}
	}

	/**
	 * Get the columns of the associated table which are to be used in the join
	 *
	 * @param type The type
	 * @param factory The SessionFactory
	 *
	 * @return The columns for the right-hand-side of the join
	 */
	public static String[] getRHSColumnNames(AssociationType type, SessionFactoryImplementor factory) {
		final String uniqueKeyPropertyName = type.getRHSUniqueKeyPropertyName();
		final Joinable joinable = type.getAssociatedJoinable( factory );
		if ( uniqueKeyPropertyName == null ) {
			return joinable.getKeyColumnNames();
		}
		else {
			return ( (OuterJoinLoadable) joinable ).getPropertyColumnNames( uniqueKeyPropertyName );
		}
	}

	private JoinHelper() {
	}
}
