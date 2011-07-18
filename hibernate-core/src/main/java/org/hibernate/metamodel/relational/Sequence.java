/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.relational;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;

/**
 * Models a database {@code SEQUENCE}.
 *
 * @author Steve Ebersole
 */
public class Sequence implements Exportable {
	private final Schema schema;
	private final String name;
	private final String qualifiedName;
	private int initialValue = 1;
	private int incrementSize = 1;

	public Sequence(Schema schema, String name) {
		this.schema = schema;
		this.name = name;
		this.qualifiedName = new ObjectName( schema, name ).toText();
	}

	public Sequence(Schema schema, String name, int initialValue, int incrementSize) {
		this( schema, name );
		this.initialValue = initialValue;
		this.incrementSize = incrementSize;
	}

	public Schema getSchema() {
		return schema;
	}

	public String getName() {
		return name;
	}

	@Override
	public String getExportIdentifier() {
		return qualifiedName;
	}

	public int getInitialValue() {
		return initialValue;
	}

	public int getIncrementSize() {
		return incrementSize;
	}

	@Override
	public String[] sqlCreateStrings(Dialect dialect) throws MappingException {
		return dialect.getCreateSequenceStrings( name, initialValue,incrementSize );
	}

	@Override
	public String[] sqlDropStrings(Dialect dialect) throws MappingException {
		return dialect.getDropSequenceStrings( name );
	}
}
