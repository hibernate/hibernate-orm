/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
