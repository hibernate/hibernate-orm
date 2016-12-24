/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.convert.spi;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.common.spi.SingularAttribute;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.query.spi.ExecutionContext;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.expression.NamedParameter;
import org.hibernate.sql.ast.expression.PositionalParameter;
import org.hibernate.sqm.domain.DomainMetamodel;
import org.hibernate.sqm.query.from.SqmAttributeJoin;
import org.hibernate.type.spi.EntityType;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class ConversionHelper {
	public static Type resolveType(
			NamedParameter parameter,
			QueryParameterBindings bindings,
			ExecutionContext executionContext) {
		final QueryParameterBinding binding = bindings.getBinding( parameter.getName() );
		if ( binding != null ) {
			if ( binding.getBindType() != null ) {
				return binding.getBindType();
			}
		}

		if ( parameter.getType() != null ) {
			return parameter.getType();
		}

		if ( binding.isMultiValued() ) {
			throw new NotYetImplementedException( "Support for Type determination for multi-valued parameters is not yet implemented" );
		}
		else if ( binding.isBound() ) {
			return executionContext.resolveParameterBindType( binding.getBindValue() );
		}

		throw new QueryException( "Unable to determine Type for named parameter [:" + parameter.getName() + "]" );
	}

	public static Type resolveType(
			PositionalParameter parameter,
			QueryParameterBindings bindings,
			ExecutionContext executionContext) {
		final QueryParameterBinding binding = bindings.getBinding( parameter.getPosition() );
		if ( binding != null ) {
			if ( binding.getBindType() != null ) {
				return binding.getBindType();
			}
		}

		if ( parameter.getType() != null ) {
			return parameter.getType();
		}

		if ( binding != null && binding.isMultiValued() ) {
			throw new NotYetImplementedException( "Support for Type determination for multi-valued parameters is not yet implemented" );
		}
		else if ( binding != null && binding.isBound() ) {
			return executionContext.resolveParameterBindType( binding.getBindValue() );
		}

		throw new QueryException( "Unable to determine Type for positional parameter [?" + parameter.getPosition() + "]" );
	}

	private ConversionHelper() {
	}

	public static EntityPersister extractEntityPersister(
			SqmAttributeJoin joinedFromElement,
			SessionFactoryImplementor factory) {
		if ( joinedFromElement.getIntrinsicSubclassIndicator() != null ) {
			return (EntityPersister) joinedFromElement.getIntrinsicSubclassIndicator();
		}

		// assume the fact that the attribute/type are entity has already been validated
		final EntityType entityType = (EntityType) ( (SingularAttribute) joinedFromElement.getAttributeBinding().getAttribute() ).getOrmType();
		final String entityName = entityType.getAssociatedEntityName( factory );
		return factory.getMetamodel().entityPersister( entityName );
	}
}
