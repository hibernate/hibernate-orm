/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

/**
 * Models information about a downcast (TREAT AS).
 *
 * @author Steve Ebersole
 */
public class SqmDowncast {
	private final EntityTypeDescriptor downcastTarget;
	private boolean intrinsic;

	public SqmDowncast(EntityTypeDescriptor downcastTarget) {
		this( downcastTarget, false );
	}

	public SqmDowncast(EntityTypeDescriptor downcastTarget, boolean intrinsic) {
		this.downcastTarget = downcastTarget;
		this.intrinsic = intrinsic;
	}

	public EntityTypeDescriptor getTargetType() {
		return downcastTarget;
	}

	public boolean isIntrinsic() {
		return intrinsic;
	}

	public void makeIntrinsic() {
		// one-way toggle
		intrinsic = true;
	}
}
