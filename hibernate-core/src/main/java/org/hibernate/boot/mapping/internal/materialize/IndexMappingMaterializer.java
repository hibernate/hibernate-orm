/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.internal.ColumnNameHelper;
import org.hibernate.boot.model.naming.internal.ConstraintNamingHelper;
import org.hibernate.boot.model.naming.internal.ImplicitNamingContextImpl;
import org.hibernate.boot.model.naming.spi.IndexNamingInput;
import org.hibernate.boot.model.naming.spi.IndexTermNamingInput;
import org.hibernate.boot.model.naming.spi.IndexTermNamingInput.Order;
import org.hibernate.boot.model.naming.spi.NamedTableNamingInput;
import org.hibernate.boot.model.naming.spi.NamingNamePair;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.unique.spi.UniqueKeyRepresentation;
import org.hibernate.dialect.unique.spi.UniqueKeyRepresentationRequest;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.PhysicalTable;
import org.hibernate.mapping.Selectable;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

/// Resolves index declarations after column binding, then selects representation and names once.
///
/// @author Steve Ebersole
public final class IndexMappingMaterializer {
	private IndexMappingMaterializer() {}

	public static void materializeIndex(ResolvedIndex declaration) {
		declaration.metadataBuildingContext().getMetadataCollector().getRelationalModelCorrespondences()
				.indexCandidates().add( declaration );
	}

	public static void finishIndexes(MetadataBuildingContext context) {
		final var registry = context.getMetadataCollector().getRelationalModelCorrespondences();
		final Map<PhysicalTable, Map<LogicalName, String>> explicitNames = new IdentityHashMap<>();
		final Map<PhysicalTable, List<Candidate>> candidates = new LinkedHashMap<>();
		for ( var declaration : registry.indexCandidates() ) {
			final var terms = IndexColumnList.parse( declaration.columnList(), declaration.sourceRole() );
			if ( terms.isEmpty() ) { continue; }
			if ( declaration.name() != null ) {
				final var name = context.getMetadataCollector().getDatabase().toLogicalName( declaration.name(), true );
				final var previous = explicitNames.computeIfAbsent( declaration.table(), ignored -> new LinkedHashMap<>() )
						.putIfAbsent( name, declaration.sourceRole() );
				if ( previous != null ) {
					if ( previous.equals( declaration.sourceRole() ) ) { continue; }
					throw new AnnotationException( "Duplicate explicit @Index name '" + declaration.name()
							+ "' on table '" + declaration.table().getName() + "' declared at "
							+ previous + " and " + declaration.sourceRole() );
				}
			}
			final var candidate = resolve( declaration, terms );
			final var onTable = candidates.computeIfAbsent( declaration.table(), ignored -> new ArrayList<>() );
			if ( declaration.name() == null && onTable.stream().anyMatch( other ->
					other.declaration.name() == null && other.definition.equals( candidate.definition ) ) ) { continue; }
			onTable.add( candidate );
		}
		registry.indexCandidates().clear();
		for ( var entry : candidates.entrySet() ) {
			final Map<PhysicalName, Definition> names = new LinkedHashMap<>();
			for ( var index : entry.getKey().getIndexes().values() ) {
				names.put( ColumnNameHelper.physicalName( index.getQuotedName( context.getMetadataCollector().getDatabase().getDialect() ),
						context.getMetadataCollector().getDatabase() ), definition( index ) );
			}
			for ( var candidate : entry.getValue() ) {
				if ( candidate.uniqueKey ) {
					UniqueKeyMappingMaterializer.materializeUniqueKey( ResolvedUniqueKey.index(
							candidate.declaration, candidate.selectables.stream().map( Column.class::cast ).toList(),
							candidate.input.terms().stream().map( term -> ordering( term.order() ) ).toList() ) );
					continue;
				}
				final var declaration = candidate.declaration;
				final var database = declaration.metadataBuildingContext().getMetadataCollector().getDatabase();
				final var logical = declaration.name() == null
						? declaration.metadataBuildingContext().getBuildingPlan().getImplicitNamingStrategy().determineIndexName(
								candidate.input, ImplicitNamingContextImpl.from( declaration.metadataBuildingContext() ) )
						: database.toLogicalName( declaration.name(), true );
				final var rendered = ConstraintNamingHelper.resolveLogical( logical, ConstraintNamingHelper.Kind.INDEX,
						declaration.metadataBuildingContext() );
				final var name = ColumnNameHelper.physicalName( rendered, database );
				final var previous = names.putIfAbsent( name, candidate.definition );
				if ( previous != null ) {
					if ( !previous.equals( candidate.definition ) ) {
						throw new MappingException( "Index naming collision on table '" + declaration.table().getName()
								+ "' for name '" + rendered + "' at " + declaration.sourceRole()
								+ ": " + previous + " versus " + candidate.definition
								+ ". Supply distinct explicit names or a custom naming strategy." );
					}
					continue;
				}
				final var index = declaration.table().getOrCreateIndex( rendered );
				index.setUnique( declaration.unique() );
				index.setType( declaration.type() );
				index.setUsing( declaration.using() );
				index.setOptions( declaration.options() );
				for ( int i = 0; i < candidate.selectables.size(); i++ ) {
					index.addColumn( candidate.selectables.get( i ), ordering( candidate.input.terms().get( i ).order() ) );
				}
			}
		}
	}

	private static Candidate resolve(ResolvedIndex declaration, List<IndexColumnList.Term> terms) {
		final var context = declaration.metadataBuildingContext();
		final var database = context.getMetadataCollector().getDatabase();
		final var columns = context.getMetadataCollector().getRelationalModelCorrespondences().columnNames();
		final var selectables = new ArrayList<Selectable>();
		final var inputs = new ArrayList<IndexTermNamingInput>();
		for ( var term : terms ) {
			if ( term.expression() ) {
				selectables.add( new Formula( term.text() ) );
				inputs.add( new IndexTermNamingInput.ExpressionTerm( term.text(), term.order() ) );
			}
			else {
				final var requested = database.toLogicalName( term.text() );
				Column column;
				LogicalName logical;
				if ( AttributeColumnReference.isRoleReference( term.text() ) ) {
					column = new AttributeColumnReference( declaration.table(), context, declaration.entityName(),
							declaration.sourceRole(), declaration.collectionRole() ).resolveRole( term.text() );
					logical = columns.findDeclarationName( declaration.table(), column );
				}
				else {
					column = columns.findPhysicalColumn( declaration.table(), requested );
					if ( column != null ) { logical = columns.selectReferenceName( declaration.table(), column, requested ); }
					else {
						column = declaration.table().getColumn( ColumnNameHelper.physicalName( term.text(), database ) );
						logical = column == null ? null : columns.findDeclarationName( declaration.table(), column );
					}
				}
				if ( column == null && declaration.entityName() != null ) {
					column = new AttributeColumnReference( declaration.table(), context,
							declaration.entityName(), declaration.sourceRole(), declaration.collectionRole() ).resolve( term.text() );
					logical = column == null ? null : columns.findDeclarationName( declaration.table(), column );
				}
				if ( column == null ) {
					throw new AnnotationException( "Index '" + declaration.name() + "' on table '" + declaration.table().getName()
							+ "' references unknown column '" + term.text() + "' at " + declaration.sourceRole() );
				}
				if ( logical == null ) { throw new MappingException( "Missing logical index column dependency: " + term.text() ); }
				selectables.add( column );
				inputs.add( new IndexTermNamingInput.ColumnTerm( new NamingNamePair( logical, column.getPhysicalName() ), term.text(), term.order() ) );
			}
		}
		if ( declaration.logicalTableName() == null ) {
			throw new MappingException( "Missing logical index table dependency at " + declaration.sourceRole() );
		}
		final var input = new IndexNamingInput( new NamedTableNamingInput( new NamingNamePair(
				declaration.logicalTableName(), declaration.table().getPhysicalName().objectName() ) ),
				inputs, declaration.unique(), declaration.type(), declaration.using() );
		final var definition = new Definition( java.util.stream.IntStream.range( 0, inputs.size() )
				.mapToObj( i -> identity( selectables.get( i ), inputs.get( i ).order() ) ).toList(),
				declaration.unique(), declaration.type(), declaration.using(), declaration.options() );
		return new Candidate( declaration, List.copyOf( selectables ), input, definition, asUniqueKey( declaration, terms ) );
	}

	private static boolean asUniqueKey(ResolvedIndex declaration, List<IndexColumnList.Term> terms) {
		return declaration.unique() && declaration.metadataBuildingContext().getMetadataCollector().getDatabase().getDialect()
				.getUniqueDelegate().representation( new UniqueKeyRepresentationRequest(
						terms.stream().anyMatch( IndexColumnList.Term::expression ), declaration.type() != null, declaration.using() != null ) )
				== UniqueKeyRepresentation.CONSTRAINT;
	}

	private static String ordering(Order order) { return order == Order.UNSPECIFIED ? null : order.name().toLowerCase( java.util.Locale.ROOT ); }

	private static TermIdentity identity(Selectable term, Order order) {
		return term instanceof Column column ? new TermIdentity( column.getPhysicalName(), null, order )
				: new TermIdentity( null, term.getText(), order );
	}

	private static Definition definition(Index index) {
		return new Definition( index.getSelectables().stream().map( term -> {
			final var direction = index.getSelectableOrderMap().get( term );
			return identity( term, direction == null ? Order.UNSPECIFIED : Order.valueOf( direction.toUpperCase( java.util.Locale.ROOT ) ) );
		} ).toList(), index.isUnique(), emptyToNull( index.getType() ), emptyToNull( index.getUsing() ), emptyToNull( index.getOptions() ) );
	}

	private static String emptyToNull(String value) { return value == null || value.isEmpty() ? null : value; }
	private record TermIdentity(PhysicalName column, String expression, Order order) {}
	private record Definition(List<TermIdentity> terms, boolean unique, String type, String using, String options) {}
	private record Candidate(ResolvedIndex declaration, List<Selectable> selectables, IndexNamingInput input,
			Definition definition, boolean uniqueKey) {}
}
