/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain.internal;

import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.ValueMapping;
import org.hibernate.boot.model.domain.spi.EntityMappingHierarchyImplementor;
import org.hibernate.boot.model.domain.spi.IdentifiableTypeMappingImplementor;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.metamodel.model.domain.RepresentationMode;

/**
 * @author Chris Cranford
 */
public class EntityMappingHierarchyImpl implements EntityMappingHierarchyImplementor {

	private IdentifiableTypeMapping rootIdentifiableType;

	private PersistentAttributeMapping identifierAttributeMapping;
	private EmbeddedValueMapping identifierEmbeddedValueMapping;
	private PersistentAttributeMapping versionAttributeMapping;
	private ValueMapping discriminatorValueMapping;

	private OptimisticLockStyle optimisticLockStyle;
	private RepresentationMode explicitRepresentationMode;

	private boolean embeddedIdentifier;

	public EntityMappingHierarchyImpl() {
	}

	@Override
	public IdentifiableTypeMappingImplementor getRootType() {
		return (IdentifiableTypeMappingImplementor) this.rootIdentifiableType;
	}

	@Override
	public void setRootType(IdentifiableTypeMapping rootIdentifiableType) {
		this.rootIdentifiableType = rootIdentifiableType;
	}

	@Override
	public PersistentAttributeMapping getIdentifierAttributeMapping() {
		return identifierAttributeMapping;
	}

	@Override
	public void setIdentifierAttributeMapping(PersistentAttributeMapping identifierAttributeMapping) {
		this.identifierAttributeMapping = identifierAttributeMapping;
	}

	@Override
	public EmbeddedValueMapping getIdentifierEmbeddedValueMapping() {
		return identifierEmbeddedValueMapping;
	}

	@Override
	public void setIdentifierEmbeddedValueMapping(EmbeddedValueMapping identifierEmbeddedValueMapping) {
		this.identifierEmbeddedValueMapping = identifierEmbeddedValueMapping;
	}

	@Override
	public PersistentAttributeMapping getVersionAttributeMapping() {
		return versionAttributeMapping;
	}

	@Override
	public void setVersionAttributeMapping(PersistentAttributeMapping versionAttributeMapping) {
		this.versionAttributeMapping = versionAttributeMapping;
	}

	@Override
	public boolean hasVersionAttributeMapping() {
		return versionAttributeMapping != null;
	}

	@Override
	public ValueMapping getDiscriminatorMapping() {
		return discriminatorValueMapping;
	}

	@Override
	public void setDiscriminatorMapping(ValueMapping discriminatorMapping) {
		this.discriminatorValueMapping = discriminatorMapping;
	}

	@Override
	public boolean hasIdentifierAttributeMapping() {
		return identifierAttributeMapping != null;
	}

	@Override
	public boolean hasIdentifierMapper() {
		return identifierEmbeddedValueMapping != null;
	}

	@Override
	public boolean hasEmbeddedIdentifier() {
		return embeddedIdentifier;
	}

	@Override
	public void setEmbeddedIdentifier(boolean embeddedIdentifier) {
		this.embeddedIdentifier = embeddedIdentifier;
	}

	@Override
	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
	}

	@Override
	public void setOptimisticLockStyle(OptimisticLockStyle optimisticLockStyle) {
		this.optimisticLockStyle = optimisticLockStyle;
	}

	@Override
	public RepresentationMode getExplicitRepresentationMode() {
		return explicitRepresentationMode;
	}

	@Override
	public void setExplicitRepresentationMode(RepresentationMode explicitRepresentationMode) {
		this.explicitRepresentationMode = explicitRepresentationMode;
	}
}
