/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.internal;

import org.hibernate.persister.common.spi.AbstractAttributeImpl;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.SingularAttributeImplementor;
import org.hibernate.persister.embeddable.spi.EmbeddablePersister;
import org.hibernate.sqm.domain.ManagedType;
import org.hibernate.sqm.domain.SingularAttribute;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class SingularAttributeEmbedded
		extends AbstractAttributeImpl
		implements SingularAttributeImplementor {

	private final EmbeddablePersister embeddablePersister;

	public SingularAttributeEmbedded(
			ManagedType declaringType,
			String attributeName,
			EmbeddablePersister embeddablePersister) {
		super( declaringType, attributeName );
		this.embeddablePersister = embeddablePersister;
	}

	@Override
	public SingularAttribute.Classification getAttributeTypeClassification() {
		return SingularAttribute.Classification.EMBEDDED;
	}

	@Override
	public org.hibernate.sqm.domain.Type getType() {
		return null;
	}

	@Override
	public boolean isId() {
		return false;
	}

	@Override
	public boolean isVersion() {
		return false;
	}

	@Override
	public org.hibernate.sqm.domain.Type getBoundType() {
		return embeddablePersister;
	}

	@Override
	public EmbeddablePersister asManagedType() {
		return (EmbeddablePersister) getBoundType();
	}

	@Override
	public Type getOrmType() {
		return embeddablePersister.getOrmType();
	}

	@Override
	public Column[] getColumns() {
		return embeddablePersister.collectColumns();
	}
}
