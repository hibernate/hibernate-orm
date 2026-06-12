/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.meta;

import org.hibernate.AssertionFailure;
import org.hibernate.Incubating;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.sql.model.TableMapping;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/// Immutable descriptor for a table involved in a mutation.
///
/// Provides metadata needed for mutation execution.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public class EntityTableDescriptor implements TableDescriptor, Serializable {
	private final String name;
	private final int relativePosition;
	private final boolean identifierTable;
	private final boolean optional;
	private final boolean inverse;
	private final boolean cascadeDeleteEnabled;
	private final TableMapping.MutationDetails insertDetails;
	private final TableMapping.MutationDetails updateDetails;
	private final TableMapping.MutationDetails deleteDetails;

	private boolean selfReferential;
	private boolean uniqueConstraints;
	private List<ColumnDescriptor> columns;
	private List<AttributeMapping> attributes;
	private Map<AttributeMapping,List<Integer>> attributeColumnIndexes;
	private TableKeyDescriptor keyDescriptor;

	/// @param name Table name from the mapping model.
	/// @param relativePosition Position within the target's grouping of tables.
	/// @param columns The columns contained on this table.
	/// @param attributes The attributes mapped to this table.
	/// @param attributeColumnIndexes A mapping of attribute to the indices
	///        (relative to [#columns()]) for the columns that attribute maps to.
	public EntityTableDescriptor(
			String name,
			int relativePosition,
			boolean isIdentifierTable,
			boolean isOptional,
			boolean isInverse,
			boolean isSelfReferential,
			boolean hasUniqueConstraints,
			boolean cascadeDeleteEnabled,
			TableMapping.MutationDetails insertDetails,
			TableMapping.MutationDetails updateDetails,
			TableMapping.MutationDetails deleteDetails,
			List<ColumnDescriptor> columns,
			List<AttributeMapping> attributes,
			Map<AttributeMapping,List<Integer>> attributeColumnIndexes,
			TableKeyDescriptor keyDescriptor) {
		this(
				name,
				relativePosition,
				isIdentifierTable,
				isOptional,
				isInverse,
				cascadeDeleteEnabled,
				insertDetails,
				updateDetails,
				deleteDetails
		);
		initializeGraphDetails(
				isSelfReferential,
				hasUniqueConstraints,
				columns,
				attributes,
				attributeColumnIndexes,
				keyDescriptor
		);
	}

	protected EntityTableDescriptor(
			String name,
			int relativePosition,
			boolean isIdentifierTable,
			boolean isOptional,
			boolean isInverse,
			boolean cascadeDeleteEnabled,
			TableMapping.MutationDetails insertDetails,
			TableMapping.MutationDetails updateDetails,
			TableMapping.MutationDetails deleteDetails) {
		this.name = name;
		this.relativePosition = relativePosition;
		this.identifierTable = isIdentifierTable;
		this.optional = isOptional;
		this.inverse = isInverse;
		this.cascadeDeleteEnabled = cascadeDeleteEnabled;
		this.insertDetails = insertDetails;
		this.updateDetails = updateDetails;
		this.deleteDetails = deleteDetails;
	}

	protected void initializeGraphDetails(
			boolean isSelfReferential,
			boolean hasUniqueConstraints,
			List<ColumnDescriptor> columns,
			List<AttributeMapping> attributes,
			Map<AttributeMapping,List<Integer>> attributeColumnIndexes,
			TableKeyDescriptor keyDescriptor) {
		if ( hasGraphDetails() ) {
			throw new AssertionFailure( "Entity table descriptor was already initialized" );
		}
		selfReferential = isSelfReferential;
		uniqueConstraints = hasUniqueConstraints;
		this.columns = List.copyOf( columns );
		this.attributes = attributes;
		this.attributeColumnIndexes = attributeColumnIndexes;
		this.keyDescriptor = keyDescriptor;
	}

	protected boolean hasGraphDetails() {
		return keyDescriptor != null;
	}

	private void checkGraphDetailsInitialized() {
		if ( !hasGraphDetails() ) {
			throw new AssertionFailure( "Entity table descriptor was not initialized" );
		}
	}

	/// The table's name.
	@Override
	public String name() {
		return name;
	}

	/// This table's relative position within its "table group".
	public int relativePosition() {
		return relativePosition;
	}

	public boolean isIdentifierTable() {
		return identifierTable;
	}

	@Override
	public boolean isOptional() {
		return optional;
	}

	public boolean isInverse() {
		return inverse;
	}

	@Override
	public boolean isSelfReferential() {
		checkGraphDetailsInitialized();
		return selfReferential;
	}

	@Override
	public boolean hasUniqueConstraints() {
		checkGraphDetailsInitialized();
		return uniqueConstraints;
	}

	@Override
	public boolean cascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	@Override
	public TableMapping.MutationDetails insertDetails() {
		return insertDetails;
	}

	@Override
	public TableMapping.MutationDetails updateDetails() {
		return updateDetails;
	}

	@Override
	public TableMapping.MutationDetails deleteDetails() {
		return deleteDetails;
	}

	public List<ColumnDescriptor> columns() {
		checkGraphDetailsInitialized();
		return columns;
	}

	public List<AttributeMapping> attributes() {
		checkGraphDetailsInitialized();
		return attributes;
	}

	public Map<AttributeMapping,List<Integer>> attributeColumnIndexes() {
		checkGraphDetailsInitialized();
		return attributeColumnIndexes;
	}

	@Override
	public TableKeyDescriptor keyDescriptor() {
		checkGraphDetailsInitialized();
		return keyDescriptor;
	}

	public void forEachAttributeColumn(AttributeMapping attribute, Consumer<ColumnDescriptor> consumer) {
		var columnIndexes = attributeColumnIndexes().get( attribute );
		if ( columnIndexes == null ) {
			// Attribute has no columns mapped to this table (e.g., plural attributes, or attributes with no selectables)
			return;
		}
		//noinspection ForLoopReplaceableByForEach
		for ( int i1 = 0; i1 < columnIndexes.size(); i1++ ) {
			consumer.accept( columns().get( columnIndexes.get( i1 ) ) );
		}
	}

	public void forAllColumns(Consumer<ColumnDescriptor> consumer) {
		Stream.of( columns(), keyDescriptor().columns() )
				.flatMap( List::stream )
				.forEach( consumer );
	}

	/// Find column by name.
	/// Returns null if not found.
	public ColumnDescriptor findColumn(String columnName) {
		return Stream.of( columns(), keyDescriptor().columns() )
				.flatMap( List::stream )
				.filter( col -> col.name().equals( columnName ) )
				.findFirst()
				.orElse( null );
	}

	/// Find columns by names.
	/// Returns empty if none found.
	public List<ColumnDescriptor> findColumns(String... columnNames) {
		return Stream.of( columns(), keyDescriptor().columns() )
				.flatMap( List::stream )
				.filter( (col) -> ArrayHelper.contains( columnNames, col.name() ) )
				.toList();
	}

	/// Find columns by names.
	/// Returns empty if none found.
	public List<ColumnDescriptor> findColumns(Collection<String> columnNames) {
		return Stream.of( columns(), keyDescriptor().columns() )
				.flatMap( List::stream )
				.filter( col -> columnNames.contains( col.name() ) )
				.toList();
	}

	@Override
	public int getRelativePosition() {
		return relativePosition;
	}

	@Override
	public String toString() {
		return "EntityTableDescriptor[" +
				"name=" + name +
				", relativePosition=" + relativePosition +
				", isIdentifierTable=" + identifierTable +
				", isOptional=" + optional +
				", isInverse=" + inverse +
				']';
	}
}
