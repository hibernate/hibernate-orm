/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.spi.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.spi.descriptor.java.MutabilityPlan;

/**
 * Optimize a mutable type, if the user promises not to mutable the
 * instances.
 * 
 * @author Gavin King
 * @author Steve Ebersole
 */
public class AdaptedImmutableType<T> extends AbstractSingleColumnStandardBasicType<T> {
	private final String name;

	public AdaptedImmutableType(AbstractSingleColumnStandardBasicType<T> baseMutableType) {
		super( baseMutableType.getColumnMapping().getSqlTypeDescriptor(), baseMutableType.getJavaTypeDescriptor() );
		this.name = "imm_" + baseMutableType.getName();
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public MutabilityPlan<T> getMutabilityPlan() {
		return ImmutableMutabilityPlan.INSTANCE;
	}

	public String getName() {
		return name;
	}
}
