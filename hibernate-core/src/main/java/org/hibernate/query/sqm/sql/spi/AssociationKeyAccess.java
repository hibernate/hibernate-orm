package org.hibernate.query.sqm.sql.spi;

import org.hibernate.Hibernate;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;

/// A unique key which a translation either bound directly or looked up by identifier.
public record AssociationKeyAccess(QueryParameterImplementor<?> parameter, ValuedModelPart key, boolean available) {
	public boolean matches(QueryParameterBindings bindings) {
		final var binding = bindings.getBinding( parameter );
		return !binding.isMultiValued() && available == isAvailable( binding.getBindValue(), key );
	}

	public static boolean isAvailable(Object argument, ValuedModelPart key) {
		if ( argument == null ) {
			return true;
		}
		if ( key.isVirtual() && key instanceof EmbeddableValuedModelPart composite ) {
			final var type = composite.getEmbeddableTypeDescriptor();
			for ( int i = 0; i < type.getNumberOfAttributeMappings(); i++ ) {
				if ( !Hibernate.isPropertyInitialized( argument, type.getAttributeMapping( i ).getAttributeName() ) ) {
					return false;
				}
			}
			return true;
		}
		return Hibernate.isPropertyInitialized( argument, key.getPartName() );
	}
}
