/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.procedure.internal;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;
import javax.persistence.ParameterMode;
import javax.persistence.TemporalType;

import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.procedure.ParameterBind;
import org.hibernate.procedure.ParameterMisuseException;
import org.hibernate.procedure.ParameterRegistration;
import org.hibernate.procedure.internal.ProcedureCallImpl;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.query.internal.QueryParameterImpl;
import org.hibernate.query.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.type.CalendarDateType;
import org.hibernate.type.CalendarTimeType;
import org.hibernate.type.CalendarType;
import org.hibernate.type.ProcedureParameterExtractionAware;
import org.hibernate.type.ProcedureParameterNamedBinder;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class ProcedureParameterImpl<T>
		extends QueryParameterImpl<T>
		implements ProcedureParameterImplementor<T>, ParameterRegistration<T> {
	private static final Logger log = Logger.getLogger( ProcedureParameterImpl.class );

	private final ProcedureCallImpl procedureCall;
	private final String name;
	private final Integer position;
	private final ParameterMode mode;
	private final Class<T> javaType;

	private int[] sqlTypes;
	private boolean passNullsEnabled;

	// in-flight state needed between prepare and extract
	private int startIndex;

	public ProcedureParameterImpl(
			ProcedureCallImpl procedureCall,
			String name,
			ParameterMode mode,
			Class<T> javaType,
			Type hibernateType,
			boolean initialPassNullsSetting) {
		super( hibernateType );
		this.procedureCall = procedureCall;
		this.name = name;
		this.position = null;
		this.mode = mode;
		this.javaType = javaType;
		this.passNullsEnabled = initialPassNullsSetting;

		setHibernateType( hibernateType );
	}

	public ProcedureParameterImpl(
			ProcedureCallImpl procedureCall,
			Integer position,
			ParameterMode mode,
			Class<T> javaType,
			Type hibernateType,
			boolean initialPassNullsSetting) {
		super( hibernateType );
		this.procedureCall = procedureCall;
		this.name = null;
		this.position = position;
		this.mode = mode;
		this.javaType = javaType;
		this.passNullsEnabled = initialPassNullsSetting;

		setHibernateType( hibernateType );
	}

	@Override
	public ParameterMode getMode() {
		return mode;
	}

	@Override
	public boolean isPassNullsEnabled() {
		return passNullsEnabled;
	}

	@Override
	public void enablePassingNulls(boolean enabled) {
		this.passNullsEnabled = enabled;
	}

	@Override
	public int[] getSourceLocations() {
		return new int[0];
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
	public void setHibernateType(Type expectedType) {
		super.setHibernateType( expectedType );

		if ( mode == ParameterMode.REF_CURSOR ) {
			sqlTypes = new int[] { Types.REF_CURSOR };
		}
		else {
			if ( expectedType == null ) {
				throw new IllegalArgumentException( "Type cannot be null" );
			}
			else {
				sqlTypes = expectedType.sqlTypes( procedureCall.getSession().getFactory() );
			}
		}

	}

	@Override
	public Class<T> getParameterType() {
		return javaType;
	}

	@Override
	public ParameterBind<T> getBind() {
		return (ParameterBind<T>) procedureCall.getQueryParameterBindings().getBinding( this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void bindValue(Object value) {
		getBind().setBindValue( (T) value );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void bindValue(Object value, TemporalType explicitTemporalType) {
		getBind().setBindValue( (T) value, explicitTemporalType );
	}

	@Override
	public void prepare(CallableStatement statement, int startIndex) throws SQLException {
		// initially set up the Type we will use for binding as the explicit type.
		Type typeToUse = getHibernateType();
		int[] sqlTypesToUse = sqlTypes;

		final ParameterBind bind = getBind();

		// however, for Calendar binding with an explicit TemporalType we may need to adjust this...
		if ( bind != null && bind.getExplicitTemporalType() != null ) {
			if ( Calendar.class.isInstance( bind.getValue() ) ) {
				switch ( bind.getExplicitTemporalType() ) {
					case TIMESTAMP: {
						typeToUse = CalendarType.INSTANCE;
						sqlTypesToUse = typeToUse.sqlTypes( procedureCall.getSession().getFactory() );
						break;
					}
					case DATE: {
						typeToUse = CalendarDateType.INSTANCE;
						sqlTypesToUse = typeToUse.sqlTypes( procedureCall.getSession().getFactory() );
						break;
					}
					case TIME: {
						typeToUse = CalendarTimeType.INSTANCE;
						sqlTypesToUse = typeToUse.sqlTypes( procedureCall.getSession().getFactory() );
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
							ProcedureParameterExtractionAware.class.isInstance( typeToUse )
									&& ( (ProcedureParameterExtractionAware) typeToUse ).canDoExtraction();
					if ( ! canHandleMultiParamExtraction ) {
						// it cannot...
						throw new UnsupportedOperationException(
								"Type [" + typeToUse + "] does support multi-parameter value extraction"
						);
					}
				}
				// TODO: sqlTypesToUse.length > 1 does not seem to have a working use case (HHH-10769).
				// The idea is that an embeddable/custom type can have more than one column values
				// that correspond with embeddable/custom attribute value. This does not seem to
				// be working yet. For now, if sqlTypesToUse.length > 1, then register
				// the out parameters by position (since we only have one name).
				// This will cause a failure if there are other parameters bound by
				// name and the dialect does not support "mixed" named/positional parameters;
				// e.g., Oracle.
				if ( sqlTypesToUse.length == 1 &&
						procedureCall.getParameterStrategy() == ParameterStrategy.NAMED &&
						canDoNameParameterBinding( typeToUse ) ) {
					statement.registerOutParameter( getName(), sqlTypesToUse[0] );
				}
				else {
					for ( int i = 0; i < sqlTypesToUse.length; i++ ) {
						statement.registerOutParameter( startIndex + i, sqlTypesToUse[i] );
					}
				}
			}

			if ( mode == ParameterMode.INOUT || mode == ParameterMode.IN ) {
				if ( bind == null || bind.getValue() == null ) {
					// the user did not bind a value to the parameter being processed.  This is the condition
					// defined by `passNulls` and that value controls what happens here.  If `passNulls` is
					// {@code true} we will bind the NULL value into the statement; if `passNulls` is
					// {@code false} we will not.
					//
					// Unfortunately there is not a way to reliably know through JDBC metadata whether a procedure
					// parameter defines a default value.  Deferring to that information would be the best option
					if ( isPassNullsEnabled() ) {
						log.debugf(
								"Stored procedure [%s] IN/INOUT parameter [%s] not bound and `passNulls` was set to true; binding NULL",
								procedureCall.getProcedureName(),
								this
						);
						if ( this.procedureCall.getParameterStrategy() == ParameterStrategy.NAMED && canDoNameParameterBinding( typeToUse ) ) {
							((ProcedureParameterNamedBinder) typeToUse).nullSafeSet(
									statement,
									null,
									this.getName(),
									procedureCall.getSession()
							);
						}
						else {
							typeToUse.nullSafeSet( statement, null, startIndex, procedureCall.getSession() );
						}
					}
					else {
						log.debugf(
								"Stored procedure [%s] IN/INOUT parameter [%s] not bound and `passNulls` was set to false; assuming procedure defines default value",
								procedureCall.getProcedureName(),
								this
						);
					}
				}
				else {
					if ( this.procedureCall.getParameterStrategy() == ParameterStrategy.NAMED && canDoNameParameterBinding( typeToUse ) ) {
						((ProcedureParameterNamedBinder) typeToUse).nullSafeSet(
								statement,
								bind.getValue(),
								this.getName(),
								procedureCall.getSession()
						);
					}
					else {
						typeToUse.nullSafeSet( statement, bind.getValue(), startIndex, procedureCall.getSession() );
					}
				}
			}
		}
		else {
			// we have a REF_CURSOR type param
			if ( procedureCall.getParameterStrategy() == ParameterStrategy.NAMED ) {
				procedureCall.getSession().getFactory().getServiceRegistry()
						.getService( RefCursorSupport.class )
						.registerRefCursorParameter( statement, getName() );
			}
			else {
				procedureCall.getSession().getFactory().getServiceRegistry()
						.getService( RefCursorSupport.class )
						.registerRefCursorParameter( statement, startIndex );
			}
		}
	}

	private boolean canDoNameParameterBinding(Type hibernateType) {
		final ExtractedDatabaseMetaData databaseMetaData = procedureCall.getSession()
				.getJdbcCoordinator()
				.getJdbcSessionOwner()
				.getJdbcSessionContext()
				.getServiceRegistry().getService( JdbcEnvironment.class )
				.getExtractedDatabaseMetaData();
		return
				databaseMetaData.supportsNamedParameters()
						&& ProcedureParameterNamedBinder.class.isInstance( hibernateType )
						&& ((ProcedureParameterNamedBinder) hibernateType).canDoSetting();
	}

	@Override
	public int[] getSqlTypes() {
		if ( mode == ParameterMode.REF_CURSOR ) {
			// we could use the Types#REF_CURSOR added in Java 8, but that would require requiring Java 8...
			throw new IllegalStateException( "REF_CURSOR parameters do not have a SQL/JDBC type" );
		}

		return determineHibernateType().sqlTypes( procedureCall.getSession().getFactory() );
	}

	private Type determineHibernateType() {
		final ParameterBind<T> bind = getBind();

		// if the bind defines a type, that should be the most specific...
		final Type bindType = bind.getBindType();
		if ( bindType != null ) {
			return bindType;
		}

		// Next, see if the parameter itself has an expected type, and if so use that...
		final Type paramType = getHibernateType();
		if ( paramType != null ) {
			return paramType;
		}

		// here we just have guessing games
		if ( bind.getValue() != null ) {
			return procedureCall.getSession()
					.getFactory()
					.getTypeResolver()
					.heuristicType( bind.getValue().getClass().getName() );
		}

		throw new IllegalStateException( "Unable to determine SQL type(s) - Hibernate Type not known" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public T extract(CallableStatement statement) {
		if ( mode == ParameterMode.IN ) {
			throw new ParameterMisuseException( "IN parameter not valid for output extraction" );
		}
		try {
			if ( mode == ParameterMode.REF_CURSOR ) {
				if ( procedureCall.getParameterStrategy() == ParameterStrategy.NAMED ) {
					return (T) statement.getObject( name );
				}
				else {
					return (T) statement.getObject( startIndex );
				}
			}
			else {
				final Type hibernateType = determineHibernateType();
				final int[] sqlTypes = hibernateType.sqlTypes( procedureCall.getSession().getFactory() );

				// TODO: sqlTypesToUse.length > 1 does not seem to have a working use case (HHH-10769).
				// For now, if sqlTypes.length > 1 with a named parameter, then extract
				// parameter values by position (since we only have one name).
				final boolean useNamed = sqlTypes.length == 1 &&
						procedureCall.getParameterStrategy() == ParameterStrategy.NAMED &&
						canDoNameParameterBinding( hibernateType );


				if ( ProcedureParameterExtractionAware.class.isInstance( hibernateType ) ) {
					if ( useNamed ) {
						return (T) ( (ProcedureParameterExtractionAware) hibernateType ).extract(
								statement,
								new String[] { getName() },
								procedureCall.getSession()
						);
					}
					else {
						return (T) ( (ProcedureParameterExtractionAware) hibernateType ).extract(
								statement,
								startIndex,
								procedureCall.getSession()
						);
					}
				}
				else {
					if ( useNamed ) {
						return (T) statement.getObject( name );
					}
					else {
						return (T) statement.getObject( startIndex );
					}
				}
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
