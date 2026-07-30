/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Changelog;
import org.hibernate.annotations.Temporal;
import org.hibernate.boot.model.internal.AuditHelper;
import org.hibernate.boot.model.internal.BinderHelper;
import org.hibernate.boot.model.internal.TemporalHelper;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.materialize.PrimaryTableKeyMappingMaterializer;
import org.hibernate.boot.mapping.internal.sources.CollectionSource;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.temporal.TemporalTableStrategy;

import jakarta.persistence.MappedSuperclass;

import static org.hibernate.internal.util.StringHelper.isBlank;

/// Compatibility phase for state-management mappings in the new boot pipeline.
///
/// This intentionally projects source-model state-management annotations into
/// the current `org.hibernate.mapping` contracts that runtime already consumes.
/// General binders register the source/model pairs they produce, and this phase
/// applies the helper-driven side effects after core tables, keys, collection
/// indexes, association targets, and foreign keys have been bound.  Later slices
/// can replace these helper-driven side effects with explicit resolved descriptors
/// without spreading the transition through general binders.
///
/// @since 9.0
/// @author Steve Ebersole
public class StateManagementBindingPhase {
	public interface RootEntity {
		void bindRootEntity();
	}

	public interface PropertyExclusions {
		void bindPropertyExclusions();
	}

	public interface CollectionMapping {
		void bindCollection();
	}

	public interface OneToManyAuditCollection {
		void bindOneToManyAuditCollection();
	}

	public interface Finalizer {
		void finalizeStateManagement();
	}

	static void registerRootEntity(ClassDetails classDetails, RootClass rootClass, BindingState bindingState) {
		bindingState.addStateManagementRootBinding( new RootEntityBinding( classDetails, rootClass, bindingState ) );
	}

	static void registerProperty(MemberDetails memberDetails, Property property, BindingState bindingState) {
		bindingState.addStateManagementPropertyBinding(
				new PropertyExclusionsBinding( memberDetails, property, bindingState )
		);
	}

	static void registerCollection(CollectionSource source, Collection collection, BindingState bindingState) {
		bindingState.addStateManagementCollectionBinding( new CollectionMappingBinding( source, collection, bindingState ) );
	}

	static void registerOneToManyCollection(
			CollectionSource source,
			Collection collection,
			String referencedEntityName,
			BindingState bindingState) {
		bindingState.addStateManagementOneToManyCollectionBinding(
				new OneToManyAuditCollectionBinding( source, collection, referencedEntityName, bindingState )
		);
	}

	static void process(BindingState bindingState) {
		processRootEntities( bindingState );
		processPropertiesAndCollections( bindingState );
	}

	static void processRootEntities(BindingState bindingState) {
		bindingState.runStateManagementRootBindings();
	}

	static void processPropertiesAndCollections(BindingState bindingState) {
		bindingState.runStateManagementPropertyAndCollectionBindings();
		bindingState.runStateManagementFinalizers();
	}

	private record RootEntityBinding(
			ClassDetails classDetails,
			RootClass rootClass,
			BindingState bindingState) implements RootEntity {
		@Override
		public void bindRootEntity() {
			StateManagementBindingPhase.bindRootEntity( classDetails, rootClass, bindingState );
		}
	}

	private record PropertyExclusionsBinding(
			MemberDetails memberDetails,
			Property property,
			BindingState bindingState) implements PropertyExclusions {
		@Override
		public void bindPropertyExclusions() {
			StateManagementBindingPhase.bindPropertyExclusions( memberDetails, property, bindingState );
		}
	}

	private record CollectionMappingBinding(
			CollectionSource source,
			Collection collection,
			BindingState bindingState) implements CollectionMapping {
		@Override
		public void bindCollection() {
			StateManagementBindingPhase.bindCollection( source, collection, bindingState );
		}
	}

	private record OneToManyAuditCollectionBinding(
			CollectionSource source,
			Collection collection,
			String referencedEntityName,
			BindingState bindingState) implements OneToManyAuditCollection {
		@Override
		public void bindOneToManyAuditCollection() {
			StateManagementBindingPhase.bindOneToManyAuditCollection(
					source,
					collection,
					referencedEntityName,
					bindingState
			);
		}
	}

	private static void bindRootEntity(ClassDetails classDetails, RootClass rootClass, BindingState bindingState) {
		bindTemporal( classDetails, rootClass, bindingState );
		bindAudited( classDetails, rootClass, bindingState );
	}

	private static void bindPropertyExclusions(MemberDetails memberDetails, Property property, BindingState bindingState) {
		bindTemporalExcluded( memberDetails, property, bindingState );
		bindAuditedExcluded( memberDetails, property );
	}

	private static void bindCollection(CollectionSource source, Collection collection, BindingState bindingState) {
		bindTemporalCollection( source, collection, bindingState );
		bindAuditedCollection( source, collection, bindingState );
	}

	private static void bindOneToManyAuditCollection(
			CollectionSource source,
			Collection collection,
			String referencedEntityName,
			BindingState bindingState) {
		if ( collection.isInverse() || collection.getCollectionTable() == null ) {
			return;
		}

		final MemberDetails member = source.member();
		final var context = bindingState.getMetadataBuildingContext();
		final Audited audited = extract( Audited.class, member, context );
		if ( audited != null && !member.hasDirectAnnotationUsage( Audited.Excluded.class ) ) {
			AuditHelper.bindOneToManyAuditTable(
					extract( Audited.Table.class, member, context ),
					collection,
					referencedEntityName,
					extract( Audited.CollectionTable.class, member, context ),
					context,
					bindingState
			);
		}
	}

	private static void bindTemporal(ClassDetails classDetails, RootClass rootClass, BindingState bindingState) {
		final Temporal temporal = extract( Temporal.class, classDetails, bindingState.getMetadataBuildingContext() );
		if ( temporal != null ) {
			TemporalHelper.bindTemporalColumns(
					temporal,
					rootClass,
					rootClass.getRootTable(),
					classDetails.getDirectAnnotationUsage( Temporal.HistoryTable.class ),
					classDetails.getDirectAnnotationUsage( Temporal.HistoryPartitioning.class ),
					bindingState.getMetadataBuildingContext(),
					bindingState
				);
				new PrimaryTableKeyMappingMaterializer( bindingState.getMetadataBuildingContext() )
						.finalizeRootPrimaryKey( rootClass );
			}
		}

	private static void bindAudited(ClassDetails classDetails, RootClass rootClass, BindingState bindingState) {
		final var context = bindingState.getMetadataBuildingContext();
		final Audited audited = extract( Audited.class, classDetails, context );
		if ( audited != null ) {
			AuditHelper.bindAuditTable(
					extract( Audited.Table.class, classDetails, context ),
					rootClass,
					classDetails,
					context,
					bindingState
			);
		}
		else {
			final Changelog changelog = extract( Changelog.class, classDetails, context );
			if ( changelog != null ) {
				AuditHelper.bindChangelog( changelog, rootClass, classDetails, context, bindingState );
			}
		}
	}

	private static void bindTemporalExcluded(MemberDetails memberDetails, Property property, BindingState bindingState) {
		if ( memberDetails.hasDirectAnnotationUsage( Temporal.Excluded.class ) ) {
			property.setTemporalExcluded( true );
			property.setOptimisticLocked( false );
			addTemporalExcludedColumnOptions( property, bindingState );
		}
	}

	private static void addTemporalExcludedColumnOptions(Property property, BindingState bindingState) {
		if ( bindingState.getMetadataBuildingContext().getTemporalTableStrategy() == TemporalTableStrategy.NATIVE ) {
			final String exclusion = bindingState.getDatabase()
					.getDialect()
					.getTemporalTableSupport()
					.getTemporalExclusionColumnOption();
			for ( var selectable : property.getSelectables() ) {
				if ( selectable instanceof Column column ) {
					final String existing = column.getOptions();
					column.setOptions( isBlank( existing ) ? exclusion : existing + " " + exclusion );
				}
			}
		}
	}

	private static void bindAuditedExcluded(MemberDetails memberDetails, Property property) {
		if ( memberDetails.hasDirectAnnotationUsage( Audited.Excluded.class ) ) {
			property.setAuditedExcluded( true );
			property.setOptimisticLocked( false );
		}
	}

	private static void bindTemporalCollection(
			CollectionSource source,
			Collection collection,
			BindingState bindingState) {
		if ( collection.isInverse() || collection.getCollectionTable() == null ) {
			return;
		}

		final MemberDetails member = source.member();
		final var context = bindingState.getMetadataBuildingContext();
		final Temporal temporal = extract( Temporal.class, member, context );
		if ( temporal != null && !member.hasDirectAnnotationUsage( Temporal.Excluded.class ) ) {
			TemporalHelper.bindTemporalColumns(
					temporal,
					collection,
					collection.getCollectionTable(),
					member.getDirectAnnotationUsage( Temporal.HistoryTable.class ),
					member.getDirectAnnotationUsage( Temporal.HistoryPartitioning.class ),
					context,
					bindingState
			);
			applySingleTableTemporalCollectionKey( collection );
		}
	}

	private static void applySingleTableTemporalCollectionKey(Collection collection) {
		if ( collection.isPrimaryKeyDisabled() || !collection.isAuxiliaryColumnInPrimaryKey() ) {
			return;
		}

		final var startingColumn = collection.getAuxiliaryColumn( collection.getAuxiliaryColumnInPrimaryKey() );
		if ( startingColumn == null ) {
			return;
		}

		final var primaryKey = collection.getCollectionTable().getPrimaryKey();
		if ( primaryKey != null ) {
			if ( !primaryKey.containsColumn( startingColumn ) ) {
				primaryKey.addColumn( startingColumn );
			}
		}
		else {
			for ( var uniqueKey : collection.getCollectionTable().getUniqueKeys().values() ) {
				if ( !uniqueKey.containsColumn( startingColumn ) ) {
					uniqueKey.addColumn( startingColumn );
				}
			}
		}
	}

	private static void bindAuditedCollection(
			CollectionSource source,
			Collection collection,
			BindingState bindingState) {
		if ( collection.isInverse() || collection.getCollectionTable() == null ) {
			return;
		}
		if ( source.nature() == CollectionSource.Nature.ONE_TO_MANY
				&& source.joinTable() == null
				&& !source.oneToManyJoinColumnsOrFormulas().isEmpty() ) {
			return;
		}

		final MemberDetails member = source.member();
		final var context = bindingState.getMetadataBuildingContext();
		final Audited audited = extract( Audited.class, member, context );
		if ( audited != null && !member.hasDirectAnnotationUsage( Audited.Excluded.class ) ) {
			AuditHelper.bindAuditTable(
					extract( Audited.Table.class, member, context ),
					collection,
					context,
					bindingState
			);
		}
	}

	private static <A extends Annotation> A extract(
			Class<A> annotationClass,
			ClassDetails classDetails,
			MetadataBuildingContext context) {
		final var modelsContext = context.getModelsContext();
		final var fromClass = classDetails.getAnnotationUsage( annotationClass, modelsContext );
		if ( fromClass != null ) {
			return fromClass;
		}

		ClassDetails classToCheck = classDetails.getSuperClass();
		while ( classToCheck != null ) {
			final var fromSuper = classToCheck.getAnnotationUsage( annotationClass, modelsContext );
			if ( fromSuper != null && classToCheck.hasAnnotationUsage( MappedSuperclass.class, modelsContext ) ) {
				return fromSuper;
			}
			classToCheck = classToCheck.getSuperClass();
		}

		return BinderHelper.extractFromPackage( annotationClass, classDetails, context );
	}

	private static <A extends Annotation> A extract(
			Class<A> annotationClass,
			MemberDetails memberDetails,
			MetadataBuildingContext context) {
		final var fromMember = memberDetails.getDirectAnnotationUsage( annotationClass );
		if ( fromMember != null ) {
			return fromMember;
		}

		final var modelsContext = context.getModelsContext();
		final var declaringType = memberDetails.getDeclaringType();
		final var fromClass = declaringType.getAnnotationUsage( annotationClass, modelsContext );
		return fromClass == null
				? BinderHelper.extractFromPackage( annotationClass, declaringType, context )
				: fromClass;
	}
}
