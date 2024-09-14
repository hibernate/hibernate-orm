/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.loader.ast.spi.MultiIdEntityLoader;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;

import java.util.List;

/**
 * Base support for {@link MultiIdEntityLoader} implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractMultiIdEntityLoader<T> implements MultiIdEntityLoader<T> {
	private final EntityMappingType entityDescriptor;
	private final SessionFactoryImplementor sessionFactory;
	private final EntityIdentifierMapping identifierMapping;

	public AbstractMultiIdEntityLoader(EntityMappingType entityDescriptor, SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;
		this.sessionFactory = sessionFactory;
		identifierMapping = getLoadable().getIdentifierMapping();
	}

	protected EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
	}

	protected SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	public EntityIdentifierMapping getIdentifierMapping() {
		return identifierMapping;
	}

	@Override
	public EntityMappingType getLoadable() {
		return getEntityDescriptor();
	}

	@Override
	public final <K> List<T> load(K[] ids, MultiIdLoadOptions loadOptions, EventSource session) {
		assert ids != null;
		if ( loadOptions.isOrderReturnEnabled() ) {
			return performOrderedMultiLoad( ids, loadOptions, session );
		}
		else {
			return performUnorderedMultiLoad( ids, loadOptions, session );
		}
	}

	protected abstract <K> List<T> performOrderedMultiLoad(K[] ids, MultiIdLoadOptions loadOptions, EventSource session);

	protected abstract <K> List<T> performUnorderedMultiLoad(K[] ids, MultiIdLoadOptions loadOptions, EventSource session);

}
