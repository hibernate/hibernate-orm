/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.walking.spi;

import java.util.Set;
import javax.persistence.metamodel.Attribute;

/**
* @author Steve Ebersole
*/
@Deprecated
public interface AttributeSource {
	Set<Attribute<? super X, ?>> getAttributes()
	public Iterable<AttributeDefinition> getAttributes();
}
