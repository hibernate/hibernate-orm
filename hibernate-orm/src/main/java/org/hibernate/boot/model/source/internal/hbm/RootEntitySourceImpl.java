/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNaturalIdType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.model.source.spi.IdentifiableTypeSource;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.TableSpecificationSource;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class RootEntitySourceImpl extends AbstractEntitySourceImpl {
	private final TableSpecificationSource primaryTable;

	protected RootEntitySourceImpl(
			MappingDocument sourceMappingDocument,
			JaxbHbmRootEntityType entityElement) {
		super( sourceMappingDocument, entityElement );
		this.primaryTable = Helper.createTableSource(
				sourceMappingDocument(),
				entityElement,
				this,
				entityElement.getRowid(),
				entityElement.getComment(),
				entityElement.getCheck()
		);
		afterInstantiation();
	}

	@Override
	protected void buildAttributeSources(final AttributesHelper.Callback attributeBuildingCallback) {
		final JaxbHbmNaturalIdType naturalId = jaxbEntityMapping().getNaturalId();
		if ( naturalId != null ) {
			final NaturalIdMutability naturalIdMutability = naturalId.isMutable()
					? NaturalIdMutability.MUTABLE
					: NaturalIdMutability.IMMUTABLE;

			AttributesHelper.processAttributes(
					sourceMappingDocument(),
					attributeBuildingCallback,
					naturalId.getAttributes(),
					null,
					naturalIdMutability
			);
		}

		super.buildAttributeSources( attributeBuildingCallback );
	}

	@Override
	protected JaxbHbmRootEntityType jaxbEntityMapping() {
		return (JaxbHbmRootEntityType) super.jaxbEntityMapping();
	}

	@Override
	public TableSpecificationSource getPrimaryTable() {
		return primaryTable;
	}

	@Override
	public String getDiscriminatorMatchValue() {
		return jaxbEntityMapping().getDiscriminatorValue();
	}

	@Override
	public IdentifiableTypeSource getSuperType() {
		return null;
	}
}
