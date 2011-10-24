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

import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.ValidationException;

/**
 * Basic support for {@link SimpleValue} implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSimpleValue implements SimpleValue {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, AbstractSimpleValue.class.getName());

	private final TableSpecification table;
	private final int position;
	private Datatype datatype;

	protected AbstractSimpleValue(TableSpecification table, int position) {
		this.table = table;
		this.position = position;
	}

	@Override
	public TableSpecification getTable() {
		return table;
	}

	public int getPosition() {
		return position;
	}

	@Override
	public Datatype getDatatype() {
		return datatype;
	}

	@Override
	public void setDatatype(Datatype datatype) {
		LOG.debugf( "setting datatype for column %s : %s", toLoggableString(), datatype );
		if ( this.datatype != null && ! this.datatype.equals( datatype ) ) {
			LOG.debugf( "overriding previous datatype : %s", this.datatype );
		}
		this.datatype = datatype;
	}

	@Override
	public void validateJdbcTypes(JdbcCodes typeCodes) {
		// todo : better compatibility testing...
		if ( datatype.getTypeCode() != typeCodes.nextJdbcCde() ) {
			throw new ValidationException( "Mismatched types" );
		}
	}
}
