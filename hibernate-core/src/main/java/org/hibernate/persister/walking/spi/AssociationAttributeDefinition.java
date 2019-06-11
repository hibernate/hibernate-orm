/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.walking.spi;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.spi.HydratedCompoundValueHandler;
import org.hibernate.type.AssociationType;

/**
 * @author Steve Ebersole
 */
public interface AssociationAttributeDefinition extends AttributeDefinition {
	@Override
	AssociationType getType();

	AssociationKey getAssociationKey();

	enum AssociationNature {
		ANY,
		ENTITY,
		COLLECTION
	}

	AssociationNature getAssociationNature();

	EntityDefinition toEntityDefinition();

	CollectionDefinition toCollectionDefinition();

	AnyMappingDefinition toAnyDefinition();

	FetchStrategy determineFetchPlan(LoadQueryInfluencers loadQueryInfluencers, PropertyPath propertyPath);

	CascadeStyle determineCascadeStyle();

	HydratedCompoundValueHandler getHydratedCompoundValueExtractor();
}
