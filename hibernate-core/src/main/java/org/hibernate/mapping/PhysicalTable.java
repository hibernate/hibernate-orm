/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;


import org.hibernate.relational.naming.spi.QualifiedPhysicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.boot.model.relational.Namespace;

/// Mapping for a physical database table, including secondary and collection tables.
///
/// @author Steve Ebersole
public class PhysicalTable extends NamedTable {
	public PhysicalTable(String contributor, QualifiedPhysicalName name, boolean abstractTable) {
		super( contributor, name );
		setAbstract( abstractTable );
	}

	public PhysicalTable(String contributor, Namespace namespace, PhysicalName physicalName, boolean abstractTable) {
		super( contributor, namespace, physicalName );
		setAbstract( abstractTable );
	}

	@FunctionalInterface
	public interface InitCommandProducer
			extends java.util.function.Function<SqlStringGenerationContext, InitCommand>, Serializable {
	}

	@FunctionalInterface
	public interface ResyncCommandProducer
			extends java.util.function.BiFunction<SqlStringGenerationContext, DdlTransactionIsolator, InitCommand>,
				Serializable {
	}
	private final Map<String, Index> indexes = new LinkedHashMap<>();
	private final List<CheckConstraint> checkConstraints = new ArrayList<>();
	private String type;
	private String extraDeclarations;
	private List<InitCommandProducer> initCommandProducers;
	private List<ResyncCommandProducer> resyncCommandProducers;
	private List<InitCommandProducer> resetCommandProducers;

	public Map<String, Index> getIndexes() {
		return unmodifiableMap( indexes );
	}

	public Index getOrCreateIndex(String indexName) {
		final var index =  indexes.get( indexName );
		if ( index != null ) {
			return index;
		}
		else {
			final var newIndex = new Index();
			newIndex.setName( indexName );
			newIndex.setTable( this );
			indexes.put( indexName, newIndex );
			return newIndex;
		}
	}

	public Index getIndex(String indexName) {
		return indexes.get( indexName );
	}

	public Index addIndex(Index index) {
		final var current =  indexes.get( index.getName() );
		if ( current != null ) {
			throw new MappingException( "Index " + index.getName() + " already exists" );
		}
		indexes.put( index.getName(), index );
		return index;
	}

	public void addCheck(CheckConstraint check) {
		checkConstraints.add( check );
	}

	public List<CheckConstraint> getChecks() {
		return unmodifiableList( checkConstraints );
	}

	/// @deprecated Use [#addInitCommand(InitCommandProducer)] instead.
	@Deprecated
	public void addInitCommand(InitCommand command) {
		addInitCommand( ignored -> command );
	}

	public void addInitCommand(InitCommandProducer commandProducer) {
		if ( initCommandProducers == null ) {
			initCommandProducers = new ArrayList<>();
		}
		initCommandProducers.add( commandProducer );
	}

	public List<InitCommand> getInitCommands(SqlStringGenerationContext context) {
		return initCommandProducers == null
				? emptyList()
				: initCommandProducers.stream()
						.map( producer -> producer.apply( context ) )
						.distinct()
						.toList();
	}

	public void addResyncCommand(ResyncCommandProducer commandProducer) {
		if ( resyncCommandProducers == null ) {
			resyncCommandProducers = new ArrayList<>();
		}
		resyncCommandProducers.add( commandProducer );
	}

	public List<InitCommand> getResyncCommands(SqlStringGenerationContext context, DdlTransactionIsolator isolator) {
		return resyncCommandProducers == null
				? emptyList()
				: resyncCommandProducers.stream()
						.map( producer -> producer.apply( context, isolator ) )
						.distinct()
						.toList();
	}

	public void addResetCommand(InitCommandProducer commandProducer) {
		if ( resetCommandProducers == null ) {
			resetCommandProducers = new ArrayList<>();
		}
		resetCommandProducers.add( commandProducer );
	}

	public List<InitCommand> getResetCommands(SqlStringGenerationContext context) {
		return resetCommandProducers == null
				? emptyList()
				: resetCommandProducers.stream()
						.map( producer -> producer.apply( context ) )
						.distinct()
						.toList();
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getExtraDeclarations() {
		return extraDeclarations;
	}

	public void setExtraDeclarations(String extraDeclarations) {
		this.extraDeclarations = extraDeclarations;
	}

}
