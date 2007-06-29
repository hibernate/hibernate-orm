//$Id: OuterJoinableAssociation.java 6828 2005-05-19 07:28:59Z steveebersole $
package org.hibernate.loader;

import java.util.List;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.engine.JoinHelper;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.type.AssociationType;
import org.hibernate.type.EntityType;

public final class OuterJoinableAssociation {
	private final AssociationType joinableType;
	private final Joinable joinable;
	private final String lhsAlias; // belong to other persister
	private final String[] lhsColumns; // belong to other persister
	private final String rhsAlias;
	private final String[] rhsColumns;
	private final int joinType;
	private final String on;
	private final Map enabledFilters;

	public OuterJoinableAssociation(
		AssociationType joinableType,
		String lhsAlias,
		String[] lhsColumns,
		String rhsAlias,
		int joinType,
		SessionFactoryImplementor factory,
		Map enabledFilters)
	throws MappingException {
		this.joinableType = joinableType;
		this.lhsAlias = lhsAlias;
		this.lhsColumns = lhsColumns;
		this.rhsAlias = rhsAlias;
		this.joinType = joinType;
		this.joinable = joinableType.getAssociatedJoinable(factory);
		this.rhsColumns = JoinHelper.getRHSColumnNames(joinableType, factory);
		this.on = joinableType.getOnCondition(rhsAlias, factory, enabledFilters);
		this.enabledFilters = enabledFilters; // needed later for many-to-many/filter application
	}

	public int getJoinType() {
		return joinType;
	}

	public String getRHSAlias() {
		return rhsAlias;
	}

	private boolean isOneToOne() {
		if ( joinableType.isEntityType() )  {
			EntityType etype = (EntityType) joinableType;
			return etype.isOneToOne() /*&& etype.isReferenceToPrimaryKey()*/;
		}
		else {
			return false;
		}
			
	}
	
	public AssociationType getJoinableType() {
		return joinableType;
	}
	
	public String getRHSUniqueKeyName() {
		return joinableType.getRHSUniqueKeyPropertyName();
	}

	public boolean isCollection() {
		return joinableType.isCollectionType();
	}

	public Joinable getJoinable() {
		return joinable;
	}

	public int getOwner(final List associations) {
		if ( isOneToOne() || isCollection() ) {
			return getPosition(lhsAlias, associations);
		}
		else {
			return -1;
		}
	}

	/**
	 * Get the position of the join with the given alias in the
	 * list of joins
	 */
	private static int getPosition(String lhsAlias, List associations) {
		int result = 0;
		for ( int i=0; i<associations.size(); i++ ) {
			OuterJoinableAssociation oj = (OuterJoinableAssociation) associations.get(i);
			if ( oj.getJoinable().consumesEntityAlias() /*|| oj.getJoinable().consumesCollectionAlias() */ ) {
				if ( oj.rhsAlias.equals(lhsAlias) ) return result;
				result++;
			}
		}
		return -1;
	}

	public void addJoins(JoinFragment outerjoin) throws MappingException {
		outerjoin.addJoin(
			joinable.getTableName(),
			rhsAlias,
			lhsColumns,
			rhsColumns,
			joinType,
			on
		);
		outerjoin.addJoins(
			joinable.fromJoinFragment(rhsAlias, false, true),
			joinable.whereJoinFragment(rhsAlias, false, true)
		);
	}

	public void validateJoin(String path) throws MappingException {
		if (
			rhsColumns==null || 
			lhsColumns==null ||
			lhsColumns.length!=rhsColumns.length ||
			lhsColumns.length==0
		) {
			throw new MappingException("invalid join columns for association: " + path);
		}
	}

	public boolean isManyToManyWith(OuterJoinableAssociation other) {
		if ( joinable.isCollection() ) {
			QueryableCollection persister = ( QueryableCollection ) joinable;
			if ( persister.isManyToMany() ) {
				return persister.getElementType() == other.getJoinableType();
			}
		}
		return false;
	}

	public void addManyToManyJoin(JoinFragment outerjoin, QueryableCollection collection) throws MappingException {
		String manyToManyFilter = collection.getManyToManyFilterFragment( rhsAlias, enabledFilters );
		String condition = "".equals( manyToManyFilter )
				? on
				: "".equals( on )
						? manyToManyFilter
						: on + " and " + manyToManyFilter;
		outerjoin.addJoin(
		        joinable.getTableName(),
		        rhsAlias,
		        lhsColumns,
		        rhsColumns,
		        joinType,
		        condition
		);
		outerjoin.addJoins(
			joinable.fromJoinFragment(rhsAlias, false, true),
			joinable.whereJoinFragment(rhsAlias, false, true)
		);
	}
}