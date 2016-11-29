/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.java.managed.attribute;

import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.type.spi.descriptor.java.managed.AttributeBuilderSingular;
import org.hibernate.type.spi.descriptor.java.managed.AttributeDeclarer;

/**
 * @author Steve Ebersole
 */
public class AttributeBuilderSingularStandardImpl
		extends AbstractAttributeBuilder<SingularAttribute>
		implements AttributeBuilderSingular {
	public AttributeBuilderSingularStandardImpl(AttributeDeclarer declarer, String attributeName) {
		super( declarer, attributeName );
	}

	@Override
	protected SingularAttribute generateAttribute() {
		throw new NotYetImplementedException( "Building SingularAttributes is not yet implemented" );
	}
}
