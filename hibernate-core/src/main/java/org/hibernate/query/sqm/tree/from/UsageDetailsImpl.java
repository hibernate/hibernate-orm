/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;

/**
 * @author Steve Ebersole
 */
public class UsageDetailsImpl implements MutableUsageDetails {
	private boolean usedInFrom;
	private boolean usedInSelect;

	private ManagedTypeDescriptor intrinsicSubclassIndicator;

	// todo (6.0) : is it important to "bind" these together?
	//		does it matter where which subclass downcast comes from?  If so, what about multi-treats in different locations?

	private Set<ManagedTypeDescriptor> incidentalSubclassIndicators;
	private EnumSet<DowncastLocation> downcastLocations;

	private Set<Navigable> referencedNavigables;

	public UsageDetailsImpl(SqmFrom sqmFrom) {
		if ( sqmFrom instanceof SqmRoot || sqmFrom instanceof SqmCrossJoin || sqmFrom instanceof SqmEntityJoin ) {
			// these tyes can only originate from FROM clause
			usedInFrom = true;
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// details

	@Override
	public boolean isUsedInFrom() {
		return usedInFrom;
	}

	@Override
	public boolean isUsedInSelect() {
		return usedInSelect;
	}

	@Override
	public ManagedTypeDescriptor getIntrinsicSubclassIndicator() {
		return intrinsicSubclassIndicator;
	}

	@Override
	public Collection<ManagedTypeDescriptor> getIncidentalSubclassIndicators() {
		return incidentalSubclassIndicators;
	}

	@Override
	public EnumSet<DowncastLocation> getDowncastLocations() {
		return downcastLocations;
	}

	@Override
	public Collection<Navigable> getReferencedNavigables() {
		return referencedNavigables;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// mutable


	@Override
	public void usedInFromClause() {
		this.usedInFrom = true;
	}

	@Override
	public void usedInSelectClause() {
		this.usedInSelect = true;
	}

	@Override
	public void addDownCast(
			boolean intrinsic,
			ManagedTypeDescriptor downcastType,
			DowncastLocation downcastLocation) {
		if ( intrinsic ) {
			this.intrinsicSubclassIndicator = downcastType;
		}
		else {
			if ( incidentalSubclassIndicators == null ) {
				incidentalSubclassIndicators = new HashSet<>();
			}
			incidentalSubclassIndicators.add( downcastType );
		}

		if ( downcastLocations == null ) {
			downcastLocations = EnumSet.of( downcastLocation );
		}
		else {
			downcastLocations.add( downcastLocation );
		}
	}

	@Override
	public void addReferencedNavigable(Navigable navigable) {
		if ( referencedNavigables == null ) {
			referencedNavigables = new HashSet<>();
		}
		referencedNavigables.add( navigable );
	}
}
