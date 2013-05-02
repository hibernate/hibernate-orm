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
package org.hibernate.envers.internal.synchronization.work;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.internal.entities.RelationDescription;

/**
 * A work unit that handles "fake" bidirectional one-to-many relations (mapped with {@code @OneToMany+@JoinColumn} and
 * {@code @ManyToOne+@Column(insertable=false, updatable=false)}.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class FakeBidirectionalRelationWorkUnit extends AbstractAuditWorkUnit implements AuditWorkUnit {
	private final Map<String, FakeRelationChange> fakeRelationChanges;

	/*
	 * The work unit responsible for generating the "raw" entity data to be saved.
	 */
	private final AuditWorkUnit nestedWorkUnit;

	public FakeBidirectionalRelationWorkUnit(
			SessionImplementor sessionImplementor, String entityName,
			AuditConfiguration verCfg, Serializable id,
			String referencingPropertyName, Object owningEntity,
			RelationDescription rd, RevisionType revisionType,
			Object index,
			AuditWorkUnit nestedWorkUnit) {
		super( sessionImplementor, entityName, verCfg, id, revisionType );
		this.nestedWorkUnit = nestedWorkUnit;

		// Adding the change for the relation.
		fakeRelationChanges = new HashMap<String, FakeRelationChange>();
		fakeRelationChanges.put(
				referencingPropertyName, new FakeRelationChange(
				owningEntity,
				rd,
				revisionType,
				index
		)
		);
	}

	public FakeBidirectionalRelationWorkUnit(
			FakeBidirectionalRelationWorkUnit original,
			Map<String, FakeRelationChange> fakeRelationChanges,
			AuditWorkUnit nestedWorkUnit) {
		super( original.sessionImplementor, original.entityName, original.verCfg, original.id, original.revisionType );

		this.fakeRelationChanges = fakeRelationChanges;
		this.nestedWorkUnit = nestedWorkUnit;
	}

	public FakeBidirectionalRelationWorkUnit(FakeBidirectionalRelationWorkUnit original, AuditWorkUnit nestedWorkUnit) {
		super( original.sessionImplementor, original.entityName, original.verCfg, original.id, original.revisionType );

		this.nestedWorkUnit = nestedWorkUnit;

		fakeRelationChanges = new HashMap<String, FakeRelationChange>( original.getFakeRelationChanges() );
	}

	public AuditWorkUnit getNestedWorkUnit() {
		return nestedWorkUnit;
	}

	public Map<String, FakeRelationChange> getFakeRelationChanges() {
		return fakeRelationChanges;
	}

	@Override
	public boolean containsWork() {
		return true;
	}

	@Override
	public Map<String, Object> generateData(Object revisionData) {
		// Generating data with the nested work unit. This data contains all data except the fake relation.
		// Making a defensive copy not to modify the data held by the nested work unit.
		final Map<String, Object> nestedData = new HashMap<String, Object>( nestedWorkUnit.generateData( revisionData ) );

		// Now adding data for all fake relations.
		for ( FakeRelationChange fakeRelationChange : fakeRelationChanges.values() ) {
			fakeRelationChange.generateData( sessionImplementor, nestedData );
		}

		return nestedData;
	}

	@Override
	public AuditWorkUnit merge(AddWorkUnit second) {
		return merge( this, nestedWorkUnit, second );
	}

	@Override
	public AuditWorkUnit merge(ModWorkUnit second) {
		return merge( this, nestedWorkUnit, second );
	}

	@Override
	public AuditWorkUnit merge(DelWorkUnit second) {
		return second;
	}

	@Override
	public AuditWorkUnit merge(CollectionChangeWorkUnit second) {
		return this;
	}

	@Override
	public AuditWorkUnit merge(FakeBidirectionalRelationWorkUnit second) {
		// First merging the nested work units.
		final AuditWorkUnit mergedNested = second.getNestedWorkUnit().dispatch( nestedWorkUnit );

		// Now merging the fake relation changes from both work units.
		final Map<String, FakeRelationChange> secondFakeRelationChanges = second.getFakeRelationChanges();
		final Map<String, FakeRelationChange> mergedFakeRelationChanges = new HashMap<String, FakeRelationChange>();
		final Set<String> allPropertyNames = new HashSet<String>( fakeRelationChanges.keySet() );
		allPropertyNames.addAll( secondFakeRelationChanges.keySet() );

		for ( String propertyName : allPropertyNames ) {
			mergedFakeRelationChanges.put(
					propertyName,
					FakeRelationChange.merge(
							fakeRelationChanges.get( propertyName ),
							secondFakeRelationChanges.get( propertyName )
					)
			);
		}

		return new FakeBidirectionalRelationWorkUnit( this, mergedFakeRelationChanges, mergedNested );
	}

	@Override
	public AuditWorkUnit dispatch(WorkUnitMergeVisitor first) {
		return first.merge( this );
	}

	public static AuditWorkUnit merge(
			FakeBidirectionalRelationWorkUnit frwu,
			AuditWorkUnit nestedFirst,
			AuditWorkUnit nestedSecond) {
		final AuditWorkUnit nestedMerged = nestedSecond.dispatch( nestedFirst );

		// Creating a new fake relation work unit with the nested merged data
		return new FakeBidirectionalRelationWorkUnit( frwu, nestedMerged );
	}

	/**
	 * Describes a change to a single fake bidirectional relation.
	 */
	private static class FakeRelationChange {
		private final Object owningEntity;
		private final RelationDescription rd;
		private final RevisionType revisionType;
		private final Object index;

		public FakeRelationChange(
				Object owningEntity, RelationDescription rd, RevisionType revisionType,
				Object index) {
			this.owningEntity = owningEntity;
			this.rd = rd;
			this.revisionType = revisionType;
			this.index = index;
		}

		public RevisionType getRevisionType() {
			return revisionType;
		}

		public void generateData(SessionImplementor sessionImplementor, Map<String, Object> data) {
			// If the revision type is "DEL", it means that the object is removed from the collection. Then the
			// new owner will in fact be null.
			rd.getFakeBidirectionalRelationMapper().mapToMapFromEntity(
					sessionImplementor, data,
					revisionType == RevisionType.DEL ? null : owningEntity, null
			);
			rd.getFakeBidirectionalRelationMapper().mapModifiedFlagsToMapFromEntity(
					sessionImplementor, data,
					revisionType == RevisionType.DEL ? null : owningEntity, null
			);

			// Also mapping the index, if the collection is indexed.
			if ( rd.getFakeBidirectionalRelationIndexMapper() != null ) {
				rd.getFakeBidirectionalRelationIndexMapper().mapToMapFromEntity(
						sessionImplementor, data,
						revisionType == RevisionType.DEL ? null : index, null
				);
				rd.getFakeBidirectionalRelationIndexMapper().mapModifiedFlagsToMapFromEntity(
						sessionImplementor, data,
						revisionType == RevisionType.DEL ? null : index, null
				);
			}
		}

		public static FakeRelationChange merge(FakeRelationChange first, FakeRelationChange second) {
			if ( first == null ) {
				return second;
			}
			if ( second == null ) {
				return first;
			}

            /*
			 * The merging rules are the following (revision types of the first and second changes):
             * - DEL, DEL - return any (the work units are the same)
             * - DEL, ADD - return ADD (points to new owner)
             * - ADD, DEL - return ADD (points to new owner)
             * - ADD, ADD - return second (points to newer owner)
             */
			if ( first.getRevisionType() == RevisionType.DEL || second.getRevisionType() == RevisionType.ADD ) {
				return second;
			}
			else {
				return first;
			}
		}
	}
}
