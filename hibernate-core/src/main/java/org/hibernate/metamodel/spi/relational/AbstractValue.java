/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.relational;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.ValidationException;

import org.jboss.logging.Logger;

/**
 * Basic support for {@link Value} implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractValue implements Value {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, AbstractValue.class.getName());

	private final int position;
	private JdbcDataType jdbcDataType;

	protected AbstractValue(int position) {
		this.position = position;
	}

	public int getPosition() {
		return position;
	}

	@Override
	public JdbcDataType getJdbcDataType() {
		return jdbcDataType;
	}

	public void setJdbcDataType(JdbcDataType jdbcDataType) {
		LOG.debugf( "setting jdbcDataType for column %s : %s", toLoggableString(), jdbcDataType );
		if ( this.jdbcDataType != null && ! this.jdbcDataType.equals( jdbcDataType ) ) {
			LOG.debugf( "overriding previous jdbcDataType : %s", this.jdbcDataType );
		}
		this.jdbcDataType = jdbcDataType;
	}

	@Override
	public void validateJdbcTypes(JdbcCodes typeCodes) {
		// todo : better compatibility testing...
		if ( jdbcDataType.getTypeCode() != typeCodes.nextJdbcCde() ) {
			throw new ValidationException( "Mismatched types" );
		}
	}
}
