/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/**
 * Optimize a mutable type, if the user promises not to mutable the
 * instances.
 * 
 * @author Gavin King
 * @author Steve Ebersole
 */
public class AdaptedImmutableType<T> extends AbstractSingleColumnStandardBasicType<T> {
	private final AbstractStandardBasicType<T> baseMutableType;

	public AdaptedImmutableType(AbstractStandardBasicType<T> baseMutableType) {
		super( baseMutableType.getSqlTypeDescriptor(), baseMutableType.getJavaTypeDescriptor() );
		this.baseMutableType = baseMutableType;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	protected MutabilityPlan<T> getMutabilityPlan() {
		return ImmutableMutabilityPlan.INSTANCE;
	}

	public String getName() {
		return "imm_" + baseMutableType.getName();
	}
}
