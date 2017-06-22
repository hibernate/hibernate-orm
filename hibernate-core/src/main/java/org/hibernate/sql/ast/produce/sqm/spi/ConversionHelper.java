/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.produce.sqm.spi;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.exec.spi.ParameterBindingContext;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.tree.spi.expression.GenericParameter;
import org.hibernate.sql.ast.tree.spi.expression.PositionalParameter;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.type.spi.EntityType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class ConversionHelper {
	public static ExpressableType resolveType(
			GenericParameter parameter,
			ParameterBindingContext bindingContext) {
		final QueryParameterBinding parameterBinding = parameter.resolveBinding( bindingContext );
		if ( parameterBinding != null ) {
			if ( parameterBinding.getBindType() != null ) {
				return parameterBinding.getBindType();
			}
		}

		if ( parameter.getType() != null ) {
			return parameter.getType();
		}

		if ( parameterBinding.isMultiValued() ) {
			throw new NotYetImplementedException( "Support for Type determination for multi-valued parameters is not yet implemented" );
		}
		else if ( parameterBinding.isBound() ) {
			return bindingContext.getSession().getFactory().resolveParameterBindType( parameterBinding.getBindValue() );
		}

		throw new QueryException( "Unable to determine Type for named parameter [:" + parameter.getName() + "]" );
	}

	public static Type resolveType(
			PositionalParameter parameter,
			QueryParameterBindings bindings,
			SharedSessionContractImplementor persistenceContext) {
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
			return persistenceContext.resolveParameterBindType( binding.getBindValue() );
		}

		throw new QueryException( "Unable to determine Type for positional parameter [?" + parameter.getPosition() + "]" );
	}

	private ConversionHelper() {
	}

	public static EntityDescriptor extractEntityPersister(
			SqmAttributeJoin joinedFromElement,
			SessionFactoryImplementor factory) {
		if ( joinedFromElement.getIntrinsicSubclassEntityMetadata() != null ) {
			return (EntityDescriptor) joinedFromElement.getIntrinsicSubclassEntityMetadata();
		}

		// assume the fact that the attribute/type are entity has already been validated
		final EntityType entityType = (EntityType) ( ( SingularPersistentAttribute) joinedFromElement.getAttributeReference().getReferencedNavigable() ).getOrmType();
		final String entityName = entityType.getAssociatedEntityName();
		return factory.getTypeConfiguration().resolveEntityDescriptor( entityName );
	}
}
