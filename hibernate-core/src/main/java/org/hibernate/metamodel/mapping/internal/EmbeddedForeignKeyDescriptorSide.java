/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;

/**
 * @author Steve Ebersole
 */
public class EmbeddedForeignKeyDescriptorSide implements ForeignKeyDescriptor.Side {

	private final ForeignKeyDescriptor.Nature nature;
	private final EmbeddableValuedModelPart modelPart;

	public EmbeddedForeignKeyDescriptorSide(
			ForeignKeyDescriptor.Nature nature,
			EmbeddableValuedModelPart modelPart) {
		this.nature = nature;
		this.modelPart = modelPart;
	}

	@Override
	public ForeignKeyDescriptor.Nature getNature() {
		return nature;
	}

	@Override
	public EmbeddableValuedModelPart getModelPart() {
		return modelPart;
	}
}
