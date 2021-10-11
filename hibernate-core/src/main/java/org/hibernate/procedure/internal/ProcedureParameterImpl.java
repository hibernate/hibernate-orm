/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.Objects;
import jakarta.persistence.ParameterMode;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.AbstractQueryParameter;
import org.hibernate.query.internal.BindingTypeHelper;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.type.BasicType;
import org.hibernate.type.ProcedureParameterNamedBinder;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class ProcedureParameterImpl<T> extends AbstractQueryParameter<T> implements ProcedureParameterImplementor<T> {
	private static final Logger log = Logger.getLogger( ProcedureParameterImpl.class );

	private final String name;
	private Integer position;

	private final ParameterMode mode;

	private final Class<T> javaType;


	public ProcedureParameterImpl(
			String name,
			ParameterMode mode,
			Class<T> javaType,
			AllowableParameterType<T> hibernateType) {
		super( false, hibernateType );
		this.name = name;
		this.position = null;
		this.mode = mode;
		this.javaType = javaType;
	}

	public ProcedureParameterImpl(
			Integer position,
			ParameterMode mode,
			Class<T> javaType,
			AllowableParameterType<T> hibernateType) {
		super( false, hibernateType );
		this.name = null;
		this.position = position;
		this.mode = mode;
		this.javaType = javaType;
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
	public ParameterMode getMode() {
		return mode;
	}

	@Override
	public Class<T> getParameterType() {
		return javaType;
	}

	public NamedCallableQueryMemento.ParameterMemento toMemento() {
		return session -> {
			if ( getName() != null ) {
				//noinspection unchecked
				return new ProcedureParameterImpl(
						getName(),
						getMode(),
						javaType,
						getHibernateType()
				);
			}
			else {
				//noinspection unchecked
				return new ProcedureParameterImpl(
						getPosition(),
						getMode(),
						javaType,
						getHibernateType()
				);
			}
		};
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ProcedureParameterImpl<?> that = (ProcedureParameterImpl<?>) o;
		return Objects.equals( name, that.name ) &&
				Objects.equals( position, that.position ) &&
				mode == that.mode;
	}

	@Override
	public int hashCode() {
		return Objects.hash( name, position, mode );
	}

	@Override
	public void prepare(
			CallableStatement statement,
			int startIndex,
			ProcedureCallImplementor<?> procedureCall) throws SQLException {
		final QueryParameterBinding<?> binding = procedureCall.getParameterBindings().getBinding( this );
		final TypeConfiguration typeConfiguration = procedureCall.getSession().getFactory().getTypeConfiguration();
		final AllowableParameterType<T> typeToUse = BindingTypeHelper.INSTANCE.resolveTemporalPrecision(
				binding == null || binding.getExplicitTemporalPrecision() == null
						? null
						: binding.getExplicitTemporalPrecision(),
				getHibernateType(),
				typeConfiguration
		);

		if ( mode == ParameterMode.INOUT || mode == ParameterMode.OUT ) {

//				if ( sqlTypesToUse.length > 1 ) {
//					// there is more than one column involved; see if the Hibernate Type can handle
//					// multi-param extraction...
//					final boolean canHandleMultiParamExtraction =
//							ProcedureParameterExtractionAware.class.isInstance( typeToUse )
//									&& ( (ProcedureParameterExtractionAware) typeToUse ).canDoExtraction();
//					if ( ! canHandleMultiParamExtraction ) {
//						// it cannot...
//						throw new UnsupportedOperationException(
//								"Type [" + typeToUse + "] does support multi-parameter value extraction"
//						);
//					}
//				}
			// TODO: sqlTypesToUse.length > 1 does not seem to have a working use case (HHH-10769).
			// The idea is that an embeddable/custom type can have more than one column values
			// that correspond with embeddable/custom attribute value. This does not seem to
			// be working yet. For now, if sqlTypesToUse.length > 1, then register
			// the out parameters by position (since we only have one name).
			// This will cause a failure if there are other parameters bound by
			// name and the dialect does not support "mixed" named/positional parameters;
			// e.g., Oracle.
			final JdbcType recommendedJdbcType = typeToUse.getExpressableJavaTypeDescriptor()
					.getRecommendedJdbcType( typeConfiguration.getCurrentBaseSqlTypeIndicators() );

			if ( procedureCall.getParameterStrategy() == ParameterStrategy.NAMED &&
					canDoNameParameterBinding( typeToUse, procedureCall ) ) {
				statement.registerOutParameter( getName(), recommendedJdbcType.getJdbcTypeCode() );
			}
			else {
//					for ( int i = 0; i < sqlTypesToUse.length; i++ ) {
				if ( position == null ) {
					position = startIndex;
				}
				statement.registerOutParameter( startIndex, recommendedJdbcType.getJdbcTypeCode() );
//					}
			}
		}

		if ( mode == ParameterMode.INOUT || mode == ParameterMode.IN ) {
			final ValueBinder binder;
			final BasicType basicType;
			if ( typeToUse instanceof BasicType ) {
				basicType = ( (BasicType) typeToUse );
				binder = basicType.getJdbcValueBinder();
			}
			else {
				throw new NotYetImplementedFor6Exception( getClass() );
			}
			if ( binding == null || binding.getBindValue() == null ) {
				// the user did not binding a value to the parameter being processed.  This is the condition
				// defined by `passNulls` and that value controls what happens here.  If `passNulls` is
				// {@code true} we will binding the NULL value into the statement; if `passNulls` is
				// {@code false} we will not.
				//
				// Unfortunately there is not a way to reliably know through JDBC metadata whether a procedure
				// parameter defines a default value.  Deferring to that information would be the best option
				if ( ( binding != null && binding.isBound() ) || isPassNullsEnabled() ) {
					log.debugf(
							"Stored procedure [%s] IN/INOUT parameter [%s] not bound and `passNulls` was set to true; binding NULL",
							procedureCall.getProcedureName(),
							this
					);
					if ( procedureCall.getParameterStrategy() == ParameterStrategy.NAMED
							&& canDoNameParameterBinding( typeToUse, procedureCall ) ) {
						( (ProcedureParameterNamedBinder) typeToUse ).nullSafeSet(
								statement,
								null,
								this.getName(),
								procedureCall.getSession()
						);
					}
					else {
						if ( position == null ) {
							position = startIndex;
						}
						binder.bind( statement, null, position, procedureCall.getSession() );
					}
				}
				else {
					throw new IllegalArgumentException(
							"The parameter " +
									( name != null
											? "named [" + name + "]"
											: "at position [" + position + "]" )
									+ " was not set! You need to call the setParameter method." );
				}
			}
			else {
				if ( procedureCall.getParameterStrategy() == ParameterStrategy.NAMED
						&& canDoNameParameterBinding( typeToUse, procedureCall ) ) {
					( (ProcedureParameterNamedBinder) typeToUse ).nullSafeSet(
							statement,
							binding.getBindValue(),
							this.getName(),
							procedureCall.getSession()
					);
				}
				else {
					if ( position == null ) {
						position = startIndex;
					}
					binder.bind( statement, binding.getBindValue(), position, procedureCall.getSession() );
				}
			}
		}
	}

	private boolean canDoNameParameterBinding(
			AllowableParameterType hibernateType,
			ProcedureCallImplementor<?> procedureCall) {
		final ExtractedDatabaseMetaData databaseMetaData = procedureCall.getSession()
				.getJdbcCoordinator()
				.getJdbcSessionOwner()
				.getJdbcSessionContext()
				.getServiceRegistry().getService( JdbcEnvironment.class )
				.getExtractedDatabaseMetaData();
		return
				databaseMetaData.supportsNamedParameters()
						&& ProcedureParameterNamedBinder.class.isInstance( hibernateType )
						&& ( (ProcedureParameterNamedBinder) hibernateType ).canDoSetting();
	}
}
