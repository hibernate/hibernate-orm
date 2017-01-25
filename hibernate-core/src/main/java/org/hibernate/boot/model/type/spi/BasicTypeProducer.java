/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.spi;

import org.hibernate.type.spi.BasicType;

/**
 * A "producer" for a BasicType, scoped by a {@link BasicTypeProducerRegistry} which is
 * in turn scoped to the BootstrapContext.  A BasicTypeProducer represents the information
 * needed to produce a BasicType, without actually needing to build the BasicType.  There
 * are specifically 3 types of BasicTypeProducer:<ol>
 *     <li>
 *         {@link org.hibernate.boot.model.type.internal.BasicTypeProducerInstanceImpl} - represents
 *         cases where the BasicType instance was handed to us.
 *     </li>
 *     <li>
 *         {@link org.hibernate.boot.model.type.internal.BasicTypeProducerTypeDefinitionImpl} -
 *         represents cases where we were handed a type definition
 *     </li>
 *     <li>
 *         {@link org.hibernate.boot.model.type.internal.BasicTypeProducerUnregisteredImpl} -
 *         represents cases where no explicit "type source" was specified.
 *     </li>
 * </ol>
 * <p/>
 * Additionally, BasicTypeProducer allows us to build cross-usages of the referenced
 * BasicType without having to specifically create the BasicType instance.
 * <p/>
 * Because we require that BasicTypes use real JDK classes (no alt EntityMode support)
 * it seems questionable whether we really need this BasicTypeProducer abstraction - like
 * we could just immediately build the BasicType.  But there are cases where we want to
 * delay the resolution of the BasicType until we have additional information - especially
 * in the case of BasicTypeProducerUnregisteredImpl.
 *
 * @author Steve Ebersole
 */
public interface BasicTypeProducer {
	String getName();

	BasicTypeProducer injectBasicTypeSiteContext(BasicTypeSiteContext context);

	BasicType produceBasicType();
}
