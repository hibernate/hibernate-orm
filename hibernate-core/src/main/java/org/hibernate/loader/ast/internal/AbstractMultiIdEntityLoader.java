/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.loader.ast.internal;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.loader.ast.spi.MultiIdEntityLoader;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractMultiIdEntityLoader<T> implements MultiIdEntityLoader<T>, Preparable {
	private final EntityMappingType entityDescriptor;
	private final SessionFactoryImplementor sessionFactory;

	private EntityIdentifierMapping identifierMapping;

	public AbstractMultiIdEntityLoader(EntityMappingType entityDescriptor, SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;
		this.sessionFactory = sessionFactory;
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
	public <K> List<T> load(K[] ids, MultiIdLoadOptions loadOptions, EventSource session) {
		assert ids != null;
		if ( loadOptions.isOrderReturnEnabled() ) {
			return performOrderedMultiLoad( ids, session, loadOptions );
		}
		else {
			return performUnorderedMultiLoad( ids, session, loadOptions );
		}
	}

	protected abstract <K> List<T> performOrderedMultiLoad(K[] ids, EventSource session, MultiIdLoadOptions loadOptions);

	protected abstract <K> List<T> performUnorderedMultiLoad(K[] ids, EventSource session, MultiIdLoadOptions loadOptions);

	@Override
	public void prepare() {
		identifierMapping = getLoadable().getIdentifierMapping();
	}
}
