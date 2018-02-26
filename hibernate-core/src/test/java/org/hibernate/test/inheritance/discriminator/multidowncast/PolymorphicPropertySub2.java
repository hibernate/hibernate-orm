/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator.multidowncast;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;

/**
 * @author Christian Beikov
 */
@Entity
@AssociationOverrides({
		@AssociationOverride(name = "base", joinColumns = @JoinColumn(name = "base_sub_2"))
})
public class PolymorphicPropertySub2 extends PolymorphicPropertyMapBase<PolymorphicSub2, Embeddable2> {
	private static final long serialVersionUID = 1L;

	public PolymorphicPropertySub2() {
	}
}
