/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.type.ForeignKeyDirection;

/**
 * Further contract for sources of singular associations ({@code one-to-one} and {@code many-to-one}).
 *
 * @author Steve Ebersole
 */
public interface SingularAttributeSourceToOne
		extends SingularAttributeSource,
			ForeignKeyContributingSource,
			FetchableAttributeSource,
			AssociationSource,
			CascadeStyleSource{

	public String getReferencedEntityAttributeName();
	public String getReferencedEntityName();
	public ForeignKeyDirection getForeignKeyDirection();

	@Override
	FetchCharacteristicsSingularAssociation getFetchCharacteristics();

	public boolean isUnique();

	public Boolean isEmbedXml();
}
