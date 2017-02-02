/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.BaselineAttributeInformation;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class EntityBasedBasicAttribute extends AbstractEntityBasedAttribute {
	public EntityBasedBasicAttribute(
			EntityPersister source,
			SessionFactoryImplementor factory,
			int attributeNumber,
			String attributeName,
			Type attributeType,
			BaselineAttributeInformation baselineInfo) {
		super( source, factory, attributeNumber, attributeName, attributeType, baselineInfo );
	}
}
