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
package org.hibernate.procedure.internal;

import javax.persistence.ParameterMode;
import javax.persistence.TemporalType;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;

import org.jboss.logging.Logger;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.procedure.ParameterBind;
import org.hibernate.procedure.ParameterMisuseException;
import org.hibernate.type.DateType;
import org.hibernate.type.ProcedureParameterExtractionAware;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractParameterRegistrationImpl<T> implements ParameterRegistrationImplementor<T> {
	private static final Logger log = Logger.getLogger( AbstractParameterRegistrationImpl.class );

	private final ProcedureCallImpl procedureCall;

	private final Integer position;
	private final String name;

	private final ParameterMode mode;
	private final Class<T> type;

	private ParameterBindImpl bind;

	private int startIndex;
	private Type hibernateType;
	private int[] sqlTypes;

	protected AbstractParameterRegistrationImpl(
			ProcedureCallImpl procedureCall,
			Integer position,
			Class<T> type,
			ParameterMode mode) {
		this( procedureCall, position, null, type, mode );
	}

	protected AbstractParameterRegistrationImpl(
			ProcedureCallImpl procedureCall,
			String name,
			Class<T> type,
			ParameterMode mode) {
		this( procedureCall, null, name, type, mode );
	}

	private AbstractParameterRegistrationImpl(
			ProcedureCallImpl procedureCall,
			Integer position,
			String name,
			Class<T> type,
			ParameterMode mode) {
		this.procedureCall = procedureCall;

		this.position = position;
		this.name = name;

		this.mode = mode;
		this.type = type;

		setHibernateType( session().getFactory().getTypeResolver().heuristicType( type.getName() ) );
	}

	protected SessionImplementor session() {
		return procedureCall.getSession();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Integer getPosition() {
		return position;
	}

	@Override
	public Class<T> getType() {
		return type;
	}

	@Override
	public ParameterMode getMode() {
		return mode;
	}

	@Override
	public void setHibernateType(Type type) {
		if ( type == null ) {
			throw new IllegalArgumentException( "Type cannot be null" );
		}
		this.hibernateType = type;
		this.sqlTypes = hibernateType.sqlTypes( session().getFactory() );
	}

	@Override
	public ParameterBind getParameterBind() {
		return bind;
	}

	@Override
	public void bindValue(T value) {
		validateBindability();
		this.bind = new ParameterBindImpl<T>( value );
	}

	private void validateBindability() {
		if ( ! canBind() ) {
			throw new ParameterMisuseException( "Cannot bind value to non-input parameter : " + this );
		}
	}

	private boolean canBind() {
		return mode == ParameterMode.IN || mode == ParameterMode.INOUT;
	}

	@Override
	public void bindValue(T value, TemporalType explicitTemporalType) {
		validateBindability();
		if ( explicitTemporalType != null ) {
			if ( ! isDateTimeType() ) {
				throw new IllegalArgumentException( "TemporalType should not be specified for non date/time type" );
			}
		}
		this.bind = new ParameterBindImpl<T>( value, explicitTemporalType );
	}

	private boolean isDateTimeType() {
		return Date.class.isAssignableFrom( type )
				|| Calendar.class.isAssignableFrom( type );
	}

	@Override
	public void prepare(CallableStatement statement, int startIndex) throws SQLException {
		if ( mode == ParameterMode.REF_CURSOR ) {
			throw new NotYetImplementedException( "Support for REF_CURSOR parameters not yet supported" );
		}

		this.startIndex = startIndex;
		if ( mode == ParameterMode.IN || mode == ParameterMode.INOUT || mode == ParameterMode.OUT ) {
			if ( mode == ParameterMode.INOUT || mode == ParameterMode.OUT ) {
				if ( sqlTypes.length > 1 ) {
					if ( ProcedureParameterExtractionAware.class.isInstance( hibernateType )
							&& ( (ProcedureParameterExtractionAware) hibernateType ).canDoExtraction() ) {
						// the type can handle multi-param extraction...
					}
					else {
						// it cannot...
						throw new UnsupportedOperationException(
								"Type [" + hibernateType + "] does support multi-parameter value extraction"
						);
					}
				}
				for ( int i = 0; i < sqlTypes.length; i++ ) {
					statement.registerOutParameter( startIndex + i, sqlTypes[i] );
				}
			}

			if ( mode == ParameterMode.INOUT || mode == ParameterMode.IN ) {
				if ( bind == null || bind.getValue() == null ) {
					// the user did not bind a value to the parameter being processed.  That might be ok *if* the
					// procedure as defined in the database defines a default value for that parameter.
					// Unfortunately there is not a way to reliably know through JDBC metadata whether a procedure
					// parameter defines a default value.  So we simply allow the procedure execution to happen
					// assuming that the database will complain appropriately if not setting the given parameter
					// bind value is an error.
					log.debugf(
							"Stored procedure [%s] IN/INOUT parameter [%s] not bound; assuming procedure defines default value",
							procedureCall.getProcedureName(),
							this
					);
				}
				else {
					final Type typeToUse;
					if ( bind.getExplicitTemporalType() != null && bind.getExplicitTemporalType() == TemporalType.TIMESTAMP ) {
						typeToUse = hibernateType;
					}
					else if ( bind.getExplicitTemporalType() != null && bind.getExplicitTemporalType() == TemporalType.DATE ) {
						typeToUse = DateType.INSTANCE;
					}
					else {
						typeToUse = hibernateType;
					}
					typeToUse.nullSafeSet( statement, bind.getValue(), startIndex, session() );
				}
			}
		}
		else {
			// we have a REF_CURSOR type param
			if ( procedureCall.getParameterStrategy() == ParameterStrategy.NAMED ) {
				session().getFactory().getServiceRegistry()
						.getService( RefCursorSupport.class )
						.registerRefCursorParameter( statement, getName() );
			}
			else {
				session().getFactory().getServiceRegistry()
						.getService( RefCursorSupport.class )
						.registerRefCursorParameter( statement, getPosition() );
			}
		}
	}

	public int[] getSqlTypes() {
		return sqlTypes;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T extract(CallableStatement statement) {
		if ( mode == ParameterMode.IN ) {
			throw new ParameterMisuseException( "IN parameter not valid for output extraction" );
		}
		else if ( mode == ParameterMode.REF_CURSOR ) {
			throw new ParameterMisuseException( "REF_CURSOR parameters should be accessed via results" );
		}

		try {
			if ( ProcedureParameterExtractionAware.class.isInstance( hibernateType ) ) {
				return (T) ( (ProcedureParameterExtractionAware) hibernateType ).extract( statement, startIndex, session() );
			}
			else {
				return (T) statement.getObject( startIndex );
			}
		}
		catch (SQLException e) {
			throw procedureCall.getSession().getFactory().getSQLExceptionHelper().convert(
					e,
					"Unable to extract OUT/INOUT parameter value"
			);
		}
	}
}
