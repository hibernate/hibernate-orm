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
package org.hibernate.loader.plan2.build.internal.spaces;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.plan2.spi.JoinDefinedByMetadata;
import org.hibernate.loader.plan2.spi.QuerySpace;
import org.hibernate.type.AssociationType;

/**
 * @author Steve Ebersole
 */
public class JoinImpl implements JoinDefinedByMetadata {
	private final QuerySpace leftHandSide;
	private final QuerySpace rightHandSide;

	private final String lhsPropertyName;

	private final String[] rhsColumnNames;
	private final boolean rightHandSideOptional;
	private final AssociationType joinedAssociationPropertyType;

	public JoinImpl(
			QuerySpace leftHandSide,
			String lhsPropertyName,
			QuerySpace rightHandSide,
			String[] rhsColumnNames,
			AssociationType attributeType,
			boolean rightHandSideOptional) {
		this.leftHandSide = leftHandSide;
		this.lhsPropertyName = lhsPropertyName;
		this.rightHandSide = rightHandSide;
		this.rhsColumnNames = rhsColumnNames;
		this.rightHandSideOptional = rightHandSideOptional;
		this.joinedAssociationPropertyType = attributeType;
		if ( StringHelper.isEmpty( lhsPropertyName ) ) {
			throw new IllegalArgumentException( "Incoming 'lhsPropertyName' parameter was empty" );
		}
	}

	@Override
	public QuerySpace getLeftHandSide() {
		return leftHandSide;
	}

	@Override
	public QuerySpace getRightHandSide() {
		return rightHandSide;
	}

	@Override
	public boolean isRightHandSideRequired() {
		return rightHandSideOptional;
	}

	@Override
	public String[] resolveAliasedLeftHandSideJoinConditionColumns(String leftHandSideTableAlias) {
		return getLeftHandSide().getPropertyMapping().toColumns( leftHandSideTableAlias, getJoinedAssociationPropertyName() );
	}

	@Override
	public String[] resolveNonAliasedRightHandSideJoinConditionColumns() {
		// for composite joins (joins whose rhs is a composite) we'd have no columns here.
		// processing of joins tries to root out all composite joins, so the expectation
		// is that this method would never be called on them
		if ( rhsColumnNames == null ) {
			throw new IllegalStateException(
					"rhsColumnNames were null.  Generally that indicates a composite join, in which case calls to " +
							"resolveAliasedLeftHandSideJoinConditionColumns are not allowed"
			);
		}
		return rhsColumnNames;
	}

	@Override
	public String getAnyAdditionalJoinConditions(String rhsTableAlias) {
		return null;
	}

	@Override
	public String getJoinedAssociationPropertyName() {
		return lhsPropertyName;
	}

	@Override
	public AssociationType getJoinedAssociationPropertyType() {
		return joinedAssociationPropertyType;
	}
}
