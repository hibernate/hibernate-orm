/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal.spaces;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.plan.spi.JoinDefinedByMetadata;
import org.hibernate.loader.plan.spi.QuerySpace;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class JoinImpl implements JoinDefinedByMetadata {
	private final QuerySpace leftHandSide;
	private final QuerySpace rightHandSide;

	private final String lhsPropertyName;

	private final String[] rhsColumnNames;
	private final boolean rightHandSideRequired;
	private final Type joinedPropertyType;

	public JoinImpl(
			QuerySpace leftHandSide,
			String lhsPropertyName,
			QuerySpace rightHandSide,
			String[] rhsColumnNames,
			Type joinedPropertyType,
			boolean rightHandSideRequired) {
		this.leftHandSide = leftHandSide;
		this.lhsPropertyName = lhsPropertyName;
		this.rightHandSide = rightHandSide;
		this.rhsColumnNames = rhsColumnNames;
		this.rightHandSideRequired = rightHandSideRequired;
		this.joinedPropertyType = joinedPropertyType;
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
		return rightHandSideRequired;
	}

	@Override
	public String[] resolveAliasedLeftHandSideJoinConditionColumns(String leftHandSideTableAlias) {
		return getLeftHandSide().toAliasedColumns( leftHandSideTableAlias, getJoinedPropertyName() );
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
	public String getJoinedPropertyName() {
		return lhsPropertyName;
	}

	@Override
	public Type getJoinedPropertyType() {
		return joinedPropertyType;
	}
}
