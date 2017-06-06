/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class PrimaryKey {
	private static final Logger log = Logger.getLogger( PrimaryKey.class );

	private final Table table;
	private final List<PhysicalColumn> columns = new ArrayList<>();

	public PrimaryKey(Table table) {
		this.table = table;
	}

	public List<PhysicalColumn> getColumns() {
		return Collections.unmodifiableList( columns );
	}

	public void addColumn(PhysicalColumn column) {
		log.debugf( "Adding column [%s] to primary-key for table [%s]", column.getExpression(), table );
		columns.add( column );
	}
}
