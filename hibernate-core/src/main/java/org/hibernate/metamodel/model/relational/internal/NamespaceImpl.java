/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.model.relational.spi.Namespace;
import org.hibernate.metamodel.model.relational.spi.Sequence;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.naming.Identifier;
import org.hibernate.naming.NamespaceName;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public class NamespaceImpl implements Namespace {
	private static final Logger log = Logger.getLogger( NamespaceImpl.class );

	private final NamespaceName name;

	private List<Table> tables;
	private List<Sequence> sequences;


	public NamespaceImpl(Identifier catalogName, Identifier schemaName) {
		this.name = new NamespaceName( catalogName, schemaName );
	}

	@Override
	public Identifier getCatalogName() {
		return name.getCatalog();
	}

	@Override
	public Identifier getSchemaName() {
		return name.getSchema();
	}

	@Override
	public NamespaceName getName() {
		return name;
	}

	@Override
	public Collection<Table> getTables() {
		return tables == null ? Collections.emptyList() : Collections.unmodifiableCollection( tables );
	}

	@Override
	public Table getTable(UUID tableUid) {
		for ( Table table : tables ) {
			if ( tableUid.equals( table.getUid() ) ) {
				return table;
			}
		}

		throw new HibernateException(
				"Could not locate table in namespace [" + this + "] by UUID = " + tableUid
		);
	}

	@Override
	public Collection<Sequence> getSequences() {
		return sequences == null ? Collections.emptyList() : Collections.unmodifiableCollection( sequences );
	}

	public void addTable(Table table) {
		log.debugf( "Registering Table [%s] with namespace [%s]", table, this );

		if ( tables == null ) {
			tables = new ArrayList<>();
		}

		tables.add( table );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"Namespace[catalogName=%s, schemaName=%s]",
				name.getCatalog(),
				name.getSchema()
		);
	}

	public void addSequence(Sequence sequence) {
		log.debugf( "Registering Sequence [%s] with namespace [%s]", sequence, this );

		if ( sequences == null ) {
			sequences = new ArrayList<>();
		}
		sequences.add( sequence );
	}
}
