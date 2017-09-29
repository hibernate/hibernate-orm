/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.io.Serializable;
import java.util.function.Consumer;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;

/**
 * @author Steve Ebersole
 */
public class TypeHelper {

	// todo (6.0) : rename this as `org.hibernate.metamodel.model.domain.spi.NavigableHelper`
	//		Keep as TypeHelper until all compile errors addressed.  Renaming (moving package)
	// 		will change all usages as well.

	/**
	 * {@link Consumer} of {@link PersistentAttribute}, allowing them to be filtered
	 * via {@link #shouldAccept}
	 */
	public static class FilteredAttributeConsumer implements Consumer<PersistentAttribute> {
		@Override
		public final void accept(PersistentAttribute attribute) {
			if ( shouldAccept( attribute ) ) {
				acceptAttribute( attribute );
			}
		}

		protected boolean shouldAccept(PersistentAttribute attribute) {
			return true;
		}

		protected void acceptAttribute(PersistentAttribute attribute) {
		}
	}

	public static String toLoggableString(Object[] state, ManagedTypeDescriptor<?> managedTypeDescriptor) {
		final StringBuilder buffer = new StringBuilder();
		managedTypeDescriptor.visitAttributes(
				new Consumer<PersistentAttribute>() {
					int i = 0;

					@Override
					public void accept(PersistentAttribute attribute) {
						if ( i > 0 ) {
							buffer.append( ", " );
						}

						buffer.append( attribute.getJavaTypeDescriptor().toString( state[i] ) );
						i++;
					}
				}
		);
		return buffer.toString();
	}

	public static Serializable[] disassemble(final Object[] state, final  boolean[] nonCacheable, ManagedTypeDescriptor descriptor) {
		Serializable[] disassembledState = new Serializable[state.length];
		descriptor.visitAttributes( new Consumer<PersistentAttribute>() {
			int position = 0;

			@Override
			public void accept(PersistentAttribute attribute) {
				if ( nonCacheable != null && nonCacheable[position] ) {
					disassembledState[position] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
				}
				else if ( state[position] == LazyPropertyInitializer.UNFETCHED_PROPERTY || state[position] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
					disassembledState[position] = (Serializable) state[position];
				}
				else {
					disassembledState[position] = attribute.getJavaTypeDescriptor()
							.getMutabilityPlan()
							.disassemble( state[position] );
				}
				position++;
			}
		} );
		return disassembledState;
	}

	public static Object[] assemble(final Serializable[] disassembledState, ManagedTypeDescriptor descriptor) {
		Object[] assembledProps = new Object[disassembledState.length];
		descriptor.visitAttributes( new Consumer<PersistentAttribute>() {
			int position = 0;

			@Override
			public void accept(PersistentAttribute attribute) {
				if ( disassembledState[position] == LazyPropertyInitializer.UNFETCHED_PROPERTY || disassembledState[position] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
					assembledProps[position] = disassembledState[position];
				}
				else {
					assembledProps[position] = attribute.getJavaTypeDescriptor().getMutabilityPlan().assemble(
							disassembledState[position] );
				}
				position++;
			}
		} );

		return assembledProps;
	}
}
