/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.util.function.Consumer;

import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;

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
}
