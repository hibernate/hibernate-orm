/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.util.function.Consumer;
import javax.persistence.ColumnResult;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.complete.CompleteResultBuilderBasicValuedStandard;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Implementation of ResultMappingMemento for scalar (basic) results.
 *
 * Ultimately a scalar result is defined as a column name and a BasicType with the following notes:<ul>
 *     <li>
 *         For JPA mappings, the column name is required.  For `hbm.xml` mappings, it is optional (positional)
 *     </li>
 *     <li>
 *         Ultimately, when reading values, we need the {@link BasicType}.  We know the BasicType in a few
 *         different ways:<ul>
 *             <li>
 *                 If we know an explicit Type, that is used.
 *             </li>
 *             <li>
 *                 If we do not know the Type, but do know the Java type then we determine the BasicType
 *                 based on the reported SQL type and its known mapping to the specified Java type
 *             </li>
 *             <li>
 *                 If we know neither, we use the reported SQL type and its recommended Java type to
 *                 resolve the BasicType to use
 *             </li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class ScalarResultMappingMemento implements ResultMappingMemento {

	public final String explicitColumnName;

	private final BasicType<?> explicitType;
	private final JavaTypeDescriptor<?> explicitJavaTypeDescriptor;

	/**
	 * Creation of ScalarResultMappingMemento for JPA descriptor
	 */
	public ScalarResultMappingMemento(
			ColumnResult definition,
			ResultSetMappingResolutionContext context) {
		this.explicitColumnName = definition.name();

		final Class<?> definedType = definition.type();
		if ( void.class == definedType ) {
			explicitJavaTypeDescriptor = null;
		}
		else {
			final SessionFactoryImplementor sessionFactory = context.getSessionFactory();
			final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();
			final JavaTypeDescriptorRegistry jtdRegistry = typeConfiguration.getJavaTypeDescriptorRegistry();
			this.explicitJavaTypeDescriptor = jtdRegistry.getDescriptor( definition.type() );
		}

		explicitType = null;
	}

	public ScalarResultMappingMemento(
			String explicitColumnName,
			BasicType<?> explicitType,
			ResultSetMappingResolutionContext context) {
		this.explicitColumnName = explicitColumnName;
		this.explicitType = explicitType;
		this.explicitJavaTypeDescriptor = explicitType != null
				? explicitType.getJavaTypeDescriptor()
				: null;
	}

	@Override
	public ResultBuilder resolve(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		return new CompleteResultBuilderBasicValuedStandard( this, context );
	}

	public String getExplicitColumnName() {
		return explicitColumnName;
	}

	public BasicType<?> getExplicitType() {
		return explicitType;
	}

	public JavaTypeDescriptor<?> getExplicitJavaTypeDescriptor() {
		return explicitJavaTypeDescriptor;
	}
}
