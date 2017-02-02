/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.source.spi.IdentifierSourceSimple;
import org.hibernate.boot.model.source.spi.SingularAttributeSource;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.id.EntityIdentifierNature;

/**
 * Models a simple {@code <id/>} mapping
 *
 * @author Steve Ebersole
 */
class IdentifierSourceSimpleImpl implements IdentifierSourceSimple {
	private final SingularIdentifierAttributeSourceImpl attribute;
	private final IdentifierGeneratorDefinition generatorDefinition;
	private final String unsavedValue;

	private final ToolingHintContext toolingHintContext;

	public IdentifierSourceSimpleImpl(RootEntitySourceImpl rootEntitySource) {
		final JaxbHbmRootEntityType jaxbEntityMapping = rootEntitySource.jaxbEntityMapping();
		this.attribute = new SingularIdentifierAttributeSourceImpl(
				rootEntitySource.sourceMappingDocument(),
				rootEntitySource,
				jaxbEntityMapping.getId()
		);
		this.generatorDefinition = EntityHierarchySourceImpl.interpretGeneratorDefinition(
				rootEntitySource.sourceMappingDocument(),
				rootEntitySource.getEntityNamingSource(),
				rootEntitySource.jaxbEntityMapping().getId().getGenerator()
		);
		this.unsavedValue = jaxbEntityMapping.getId().getUnsavedValue();

		this.toolingHintContext = Helper.collectToolingHints(
				rootEntitySource.getToolingHintContext(),
				jaxbEntityMapping.getId()
		);
	}

	@Override
	public SingularAttributeSource getIdentifierAttributeSource() {
		return attribute;
	}

	@Override
	public IdentifierGeneratorDefinition getIdentifierGeneratorDescriptor() {
		return generatorDefinition;
	}

	@Override
	public EntityIdentifierNature getNature() {
		return EntityIdentifierNature.SIMPLE;
	}

	@Override
	public String getUnsavedValue() {
		return unsavedValue;
	}

	@Override
	public ToolingHintContext getToolingHintContext() {
		return toolingHintContext;
	}
}
