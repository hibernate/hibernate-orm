/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

/**
 * Convenience base class for FromElement implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmFrom implements SqmFrom {
	private final SqmFromElementSpace fromElementSpace;
	private final String uid;
	private final String alias;

	private final UsageDetailsImpl usageDetails = new UsageDetailsImpl( this );

	protected AbstractSqmFrom(
			SqmFromElementSpace fromElementSpace,
			String uid,
			String alias) {
		this.fromElementSpace = fromElementSpace;
		this.uid = uid;
		this.alias = alias;
	}


	@Override
	public SqmFromElementSpace getContainingSpace() {
		return fromElementSpace;
	}

	@Override
	public String getUniqueIdentifier() {
		return uid;
	}

	@Override
	public String getIdentificationVariable() {
		return alias;
	}

	@Override
	public UsageDetails getUsageDetails() {
		return usageDetails;
	}

	@Override
	public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
		return (EntityTypeDescriptor) getUsageDetails().getIntrinsicSubclassIndicator();
	}
}
