/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.java.managed;

import javax.persistence.InheritanceType;

import org.hibernate.EntityMode;

/**
 * @author Steve Ebersole
 */
public interface EntityHierarchy {
	InheritanceStyle getInheritanceStyle();

	JavaTypeDescriptorEntityImplementor getRootEntityDescriptor();

	EntityMode getEntityMode();

	IdentifierDescriptor getIdentifierDescriptor();

	enum InheritanceStyle {
		SINGLE_TABLE( InheritanceType.SINGLE_TABLE ),
		TABLE_PER_CLASS( InheritanceType.TABLE_PER_CLASS ),
		JOINED( InheritanceType.JOINED )
		;

		private final InheritanceType jpaInheritanceType;

		InheritanceStyle(InheritanceType jpaInheritanceType) {
			this.jpaInheritanceType = jpaInheritanceType;
		}

		InheritanceType toJpaInheritanceType() {
			return jpaInheritanceType;
		}
	}
}
