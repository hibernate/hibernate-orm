package org.hibernate.ejb.metamodel;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.EmbeddableType;

/**
 * Keep contextual information related tot he metedata building process.
 * In particular keeps data than theens to be processed in a second phase.
 * @author Emmanuel Bernard
 */
class MetadataContext {
	private Map<EntityTypeDelegator<?>, Class<?>> delegators = new HashMap<EntityTypeDelegator<?>, Class<?>>();
	private Map<Class<?>, EmbeddableType<?>> embeddables = new HashMap<Class<?>, EmbeddableType<?>>();

	void addDelegator(EntityTypeDelegator<?> type, Class<?> clazz) {
		delegators.put(type, clazz);
	}

	void postProcess(Metamodel model) {
		for ( Map.Entry<EntityTypeDelegator<?>, Class<?>> entry : delegators.entrySet() ) {
			setDelegate( model, entry );
		}
	}

	private <X> void setDelegate(Metamodel model, Map.Entry<EntityTypeDelegator<?>, Class<?>> entry) {
		@SuppressWarnings( "unchecked" )
		final Class<X> entityClass = (Class<X>) entry.getValue();
		@SuppressWarnings( "unchecked" )
		final EntityTypeDelegator<X> delegator = (EntityTypeDelegator<X>) entry.getKey();
		delegator.setDelegate( model.entity( entityClass ) );
	}

	<X> void addEmbeddableType(Class<X> clazz, EmbeddableType<X> embeddableType) {
		embeddables.put( clazz, embeddableType );
	}

	Map<Class<?>, EmbeddableType<?>> getEmbeddableTypes() {
		return embeddables;
	}
}
