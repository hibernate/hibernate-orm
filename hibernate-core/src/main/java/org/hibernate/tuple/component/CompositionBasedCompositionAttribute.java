/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.component;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.EntityDefinition;
import org.hibernate.tuple.BaselineAttributeInformation;
import org.hibernate.type.CompositeType;

/**
 * @author Steve Ebersole
 */
public class CompositionBasedCompositionAttribute extends AbstractCompositionAttribute {

	public CompositionBasedCompositionAttribute(
			AbstractCompositionAttribute source,
			SessionFactoryImplementor sessionFactory,
			int entityBasedAttributeNumber,
			String attributeName,
			CompositeType attributeType,
			int columnStartPosition,
			BaselineAttributeInformation baselineInfo) {
		super(
				source,
				sessionFactory,
				entityBasedAttributeNumber,
				attributeName,
				attributeType,
				columnStartPosition,
				baselineInfo
		);
	}

	@Override
	protected EntityPersister locateOwningPersister() {
		final AbstractCompositionAttribute source = (AbstractCompositionAttribute) getSource();
		if ( EntityDefinition.class.isInstance( source.getSource() ) ) {
			return EntityDefinition.class.cast( source.getSource() ).getEntityPersister();
		}
		else {
			return AbstractCompositionAttribute.class.cast( source.getSource() ).locateOwningPersister();
		}
	}
}
