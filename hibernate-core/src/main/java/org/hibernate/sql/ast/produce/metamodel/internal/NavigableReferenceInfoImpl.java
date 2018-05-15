/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.internal;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableReferenceInfo;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class NavigableReferenceInfoImpl implements NavigableReferenceInfo {
	private final NavigableContainerReferenceInfoImpl containerReference;
	private final Navigable navigable;
	private final NavigablePath path;
	private final String uniqueIdentifier;
	private final String identificationVariable;
	private final EntityTypeDescriptor intrinsicDowncastTarget;

	public NavigableReferenceInfoImpl(
			NavigableContainerReferenceInfoImpl containerReference,
			Navigable navigable,
			NavigablePath path,
			String uniqueIdentifier,
			String identificationVariable,
			EntityTypeDescriptor intrinsicDowncastTarget) {
		this.containerReference = containerReference;
		this.navigable = navigable;
		this.path = path;
		this.uniqueIdentifier = uniqueIdentifier;
		this.identificationVariable = identificationVariable;
		this.intrinsicDowncastTarget = intrinsicDowncastTarget;
	}

	@Override
	public NavigableContainerReferenceInfoImpl getNavigableContainerReferenceInfo() {
		return containerReference;
	}

	@Override
	public Navigable getReferencedNavigable() {
		return navigable;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return path;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getReferencedNavigable().getJavaTypeDescriptor();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return getReferencedNavigable().getPersistenceType();
	}

	@Override
	public Class getJavaType() {
		return getReferencedNavigable().getJavaType();
	}

	@Override
	public String getUniqueIdentifier() {
		return uniqueIdentifier;
	}

	@Override
	public String getIdentificationVariable() {
		return identificationVariable;
	}

	@Override
	public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
		return intrinsicDowncastTarget;
	}
}
