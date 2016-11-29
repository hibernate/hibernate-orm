/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.java.managed.attribute;

import javax.persistence.metamodel.PluralAttribute;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.type.spi.descriptor.java.managed.AttributeBuilderPlural;
import org.hibernate.type.spi.descriptor.java.managed.AttributeDeclarer;

/**
 * @author Steve Ebersole
 */
public class AttributeBuilderPluralStandardImpl
		extends AbstractAttributeBuilder<PluralAttribute>
		implements AttributeBuilderPlural {

	public AttributeBuilderPluralStandardImpl(AttributeDeclarer declarer, String attributeName) {
		super( declarer, attributeName );
	}

	@Override
	protected PluralAttribute generateAttribute() {
		throw new NotYetImplementedException( "Building PluralAttributes not yet implemented" );
	}
}
