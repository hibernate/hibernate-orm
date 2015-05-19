/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import javax.persistence.ParameterMode;
import javax.persistence.TemporalType;

import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.procedure.ParameterBind;
import org.hibernate.procedure.ParameterMisuseException;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.type.CalendarDateType;
import org.hibernate.type.CalendarTimeType;
import org.hibernate.type.CalendarType;
import org.hibernate.type.ProcedureParameterExtractionAware;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * Abstract implementation of ParameterRegistration/ParameterRegistrationImplementor
 *
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


	// positional constructors ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected AbstractParameterRegistrationImpl(
			ProcedureCallImpl procedureCall,
			Integer position,
			ParameterMode mode,
			Class<T> type) {
		this( procedureCall, position, null, mode, type );
	}

	protected AbstractParameterRegistrationImpl(
			ProcedureCallImpl procedureCall,
			Integer position,
			ParameterMode mode,
			Class<T> type,
			Type hibernateType) {
		this( procedureCall, position, null, mode, type, hibernateType );
	}


	// named constructors ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected AbstractParameterRegistrationImpl(
			ProcedureCallImpl procedureCall,
			String name,
			ParameterMode mode,
			Class<T> type) {
		this( procedureCall, null, name, mode, type );
	}

	protected AbstractParameterRegistrationImpl(
			ProcedureCallImpl procedureCall,
			String name,
			ParameterMode mode,
			Class<T> type,
			Type hibernateType) {
		this( procedureCall, null, name, mode, type, hibernateType );
	}


	// full constructors ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private AbstractParameterRegistrationImpl(
			ProcedureCallImpl procedureCall,
			Integer position,
			String name,
			ParameterMode mode,
			Class<T> type,
			Type hibernateType) {
		this.procedureCall = procedureCall;

		this.position = position;
		this.name = name;

		this.mode = mode;
		this.type = type;

		if ( mode == ParameterMode.REF_CURSOR ) {
			return;
		}

		setHibernateType( hibernateType );
	}

	private AbstractParameterRegistrationImpl(
			ProcedureCallImpl procedureCall,
			Integer position,
			String name,
			ParameterMode mode,
			Class<T> type) {
		this(
				procedureCall,
				position,
				name,
				mode,
				type,
				procedureCall.getSession().getFactory().getTypeResolver().heuristicType( type.getName() )
		);
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
	public Type getHibernateType() {
		return hibernateType;
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
	@SuppressWarnings("unchecked")
	public ParameterBind<T> getBind() {
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
		// initially set up the Type we will use for binding as the explicit type.
		Type typeToUse = hibernateType;
		int[] sqlTypesToUse = sqlTypes;

		// however, for Calendar binding with an explicit TemporalType we may need to adjust this...
		if ( bind != null && bind.getExplicitTemporalType() != null ) {
			if ( Calendar.class.isInstance( bind.getValue() ) ) {
				switch ( bind.getExplicitTemporalType() ) {
					case TIMESTAMP: {
						typeToUse = CalendarType.INSTANCE;
						sqlTypesToUse = typeToUse.sqlTypes( session().getFactory() );
						break;
					}
					case DATE: {
						typeToUse = CalendarDateType.INSTANCE;
						sqlTypesToUse = typeToUse.sqlTypes( session().getFactory() );
						break;
					}
					case TIME: {
						typeToUse = CalendarTimeType.INSTANCE;
						sqlTypesToUse = typeToUse.sqlTypes( session().getFactory() );
						break;
					}
				}
			}
		}

		this.startIndex = startIndex;
		if ( mode == ParameterMode.IN || mode == ParameterMode.INOUT || mode == ParameterMode.OUT ) {
			if ( mode == ParameterMode.INOUT || mode == ParameterMode.OUT ) {
				if ( sqlTypesToUse.length > 1 ) {
					// there is more than one column involved; see if the Hibernate Type can handle
					// multi-param extraction...
					final boolean canHandleMultiParamExtraction =
							ProcedureParameterExtractionAware.class.isInstance( hibernateType )
									&& ( (ProcedureParameterExtractionAware) hibernateType ).canDoExtraction();
					if ( ! canHandleMultiParamExtraction ) {
						// it cannot...
						throw new UnsupportedOperationException(
								"Type [" + hibernateType + "] does support multi-parameter value extraction"
						);
					}
				}
				for ( int i = 0; i < sqlTypesToUse.length; i++ ) {
					statement.registerOutParameter( startIndex + i, sqlTypesToUse[i] );
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
						.registerRefCursorParameter( statement, startIndex );
			}
		}
	}

	public int[] getSqlTypes() {
		if ( mode == ParameterMode.REF_CURSOR ) {
			// we could use the Types#REF_CURSOR added in Java 8, but that would require requiring Java 8...
			throw new IllegalStateException( "REF_CURSOR parameters do not have a SQL/JDBC type" );
		}
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
