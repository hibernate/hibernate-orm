/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.model.source.spi.AssociationSource;
import org.hibernate.boot.model.source.spi.AttributeSource;
import org.hibernate.boot.model.source.spi.PluralAttributeSource;

/**
 * @author Gail Badner
 * @author Steve Ebersole
 */
public abstract class AbstractPluralAssociationElementSourceImpl
		extends AbstractHbmSourceNode implements AssociationSource {
	private final PluralAttributeSource pluralAttributeSource;

	public AbstractPluralAssociationElementSourceImpl(
			MappingDocument mappingDocument,
			PluralAttributeSource pluralAttributeSource) {
		super( mappingDocument );
		this.pluralAttributeSource = pluralAttributeSource;
	}

	@Override
	public AttributeSource getAttributeSource() {
		return pluralAttributeSource;
	}

	@Override
	public boolean isMappedBy() {
		return false;
	}
}
