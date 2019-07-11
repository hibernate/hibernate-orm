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
import javax.persistence.ParameterMode;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.metamodel.model.domain.AllowableTemporalParameterType;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.query.AbstractQueryParameter;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.type.ProcedureParameterExtractionAware;
import org.hibernate.type.ProcedureParameterNamedBinder;

/**
 * @author Steve Ebersole
 */
public class ProcedureParameterImpl<T> extends AbstractQueryParameter<T> implements ProcedureParameterImplementor<T> {
	private final String name;
	private final Integer position;

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
		throw new NotYetImplementedFor6Exception( getClass() );
//		final QueryParameterBinding<?> binding = procedureCall.getParameterBindings().getBinding( this );
//
//		// initially set up the Type we will use for binding as the explicit type.
//		AllowableParameterType typeToUse = getHibernateType();
//
//		// however, for Calendar binding with an explicit TemporalType we may need to adjust this...
//		if ( binding != null && binding.getExplicitTemporalPrecision() != null ) {
//			typeToUse = ( (AllowableTemporalParameterType) typeToUse ).resolveTemporalPrecision(
//					binding.getExplicitTemporalPrecision(),
//					procedureCall.getSession().getFactory().getTypeConfiguration()
//			);
//		}
//
//		this.startIndex = startIndex;
//		if ( mode == ParameterMode.IN || mode == ParameterMode.INOUT || mode == ParameterMode.OUT ) {
//			if ( mode == ParameterMode.INOUT || mode == ParameterMode.OUT ) {
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
//				// TODO: sqlTypesToUse.length > 1 does not seem to have a working use case (HHH-10769).
//				// The idea is that an embeddable/custom type can have more than one column values
//				// that correspond with embeddable/custom attribute value. This does not seem to
//				// be working yet. For now, if sqlTypesToUse.length > 1, then register
//				// the out parameters by position (since we only have one name).
//				// This will cause a failure if there are other parameters bound by
//				// name and the dialect does not support "mixed" named/positional parameters;
//				// e.g., Oracle.
//				if ( sqlTypesToUse.length == 1 &&
//						procedureCall.getParameterStrategy() == ParameterStrategy.NAMED &&
//						canDoNameParameterBinding( typeToUse ) ) {
//					statement.registerOutParameter( getName(), sqlTypesToUse[0] );
//				}
//				else {
//					for ( int i = 0; i < sqlTypesToUse.length; i++ ) {
//						statement.registerOutParameter( startIndex + i, sqlTypesToUse[i] );
//					}
//				}
//			}
//
//			if ( mode == ParameterMode.INOUT || mode == ParameterMode.IN ) {
//				if ( binding == null || binding.getBindValue() == null ) {
//					// the user did not binding a value to the parameter being processed.  This is the condition
//					// defined by `passNulls` and that value controls what happens here.  If `passNulls` is
//					// {@code true} we will binding the NULL value into the statement; if `passNulls` is
//					// {@code false} we will not.
//					//
//					// Unfortunately there is not a way to reliably know through JDBC metadata whether a procedure
//					// parameter defines a default value.  Deferring to that information would be the best option
//					if ( isPassNullsEnabled() ) {
//						log.debugf(
//								"Stored procedure [%s] IN/INOUT parameter [%s] not bound and `passNulls` was set to true; binding NULL",
//								procedureCall.getProcedureName(),
//								this
//						);
//						if ( procedureCall.getParameterStrategy() == ParameterStrategy.NAMED && canDoNameParameterBinding( typeToUse ) ) {
//							((ProcedureParameterNamedBinder) typeToUse).nullSafeSet(
//									statement,
//									null,
//									this.getName(),
//									procedureCall.getSession()
//							);
//						}
//						else {
//							typeToUse.nullSafeSet( statement, null, startIndex, procedureCall.getSession() );
//						}
//					}
//					else {
//						log.debugf(
//								"Stored procedure [%s] IN/INOUT parameter [%s] not bound and `passNulls` was set to false; assuming procedure defines default value",
//								procedureCall.getProcedureName(),
//								this
//						);
//					}
//				}
//				else {
//					if ( procedureCall.getParameterStrategy() == ParameterStrategy.NAMED && canDoNameParameterBinding( typeToUse ) ) {
//						((ProcedureParameterNamedBinder) typeToUse).nullSafeSet(
//								statement,
//								binding.getBindValue(),
//								this.getName(),
//								procedureCall.getSession()
//						);
//					}
//					else {
//						typeToUse.nullSafeSet( statement, binding.getBindValue(), startIndex, procedureCall.getSession() );
//					}
//				}
//			}
//		}
//		else {
//			// we have a REF_CURSOR type param
//			if ( procedureCall.getParameterStrategy() == ParameterStrategy.NAMED ) {
//				procedureCall.getSession().getFactory().getServiceRegistry()
//						.getService( RefCursorSupport.class )
//						.registerRefCursorParameter( statement, getName() );
//			}
//			else {
//				procedureCall.getSession().getFactory().getServiceRegistry()
//						.getService( RefCursorSupport.class )
//						.registerRefCursorParameter( statement, startIndex );
//			}
//		}
	}
}
