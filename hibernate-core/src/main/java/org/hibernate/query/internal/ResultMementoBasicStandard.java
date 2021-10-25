/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.util.function.Consumer;
import jakarta.persistence.ColumnResult;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.ResultMementoBasic;
import org.hibernate.query.results.ResultBuilderBasicValued;
import org.hibernate.query.results.complete.CompleteResultBuilderBasicValuedStandard;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;

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
public class ResultMementoBasicStandard implements ResultMementoBasic {

	public final String explicitColumnName;

	private final BasicType<?> explicitType;
	private final JavaType<?> explicitJavaTypeDescriptor;

	/**
	 * Creation of ScalarResultMappingMemento for JPA descriptor
	 */
	public ResultMementoBasicStandard(
			ColumnResult definition,
			ResultSetMappingResolutionContext context) {
		this.explicitColumnName = definition.name();

		BasicType resolvedBasicType = null;

		final Class<?> definedType = definition.type();
		if ( void.class == definedType ) {
			explicitJavaTypeDescriptor = null;
		}
		else {
			final SessionFactoryImplementor sessionFactory = context.getSessionFactory();
			final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();

			// first see if this is a registered BasicType...
			final BasicType<Object> registeredBasicType = typeConfiguration.getBasicTypeRegistry()
					.getRegisteredType( definition.type().getName() );
			if ( registeredBasicType != null ) {
				this.explicitJavaTypeDescriptor = registeredBasicType.getJavaTypeDescriptor();
			}
			else {
				final JavaTypeRegistry jtdRegistry = typeConfiguration.getJavaTypeDescriptorRegistry();
				final JavaType<Object> registeredJtd = jtdRegistry.getDescriptor( definition.type() );
				final ManagedBeanRegistry beanRegistry = sessionFactory.getServiceRegistry().getService( ManagedBeanRegistry.class );
				if ( BasicType.class.isAssignableFrom( registeredJtd.getJavaTypeClass() ) ) {
					final ManagedBean<BasicType<?>> typeBean = (ManagedBean) beanRegistry.getBean( registeredJtd.getJavaTypeClass() );
					resolvedBasicType = typeBean.getBeanInstance();
					this.explicitJavaTypeDescriptor = resolvedBasicType.getJavaTypeDescriptor();
				}
				else if ( UserType.class.isAssignableFrom( registeredJtd.getJavaTypeClass() ) ) {
					final ManagedBean<UserType<?>> userTypeBean = (ManagedBean) beanRegistry.getBean( registeredJtd.getJavaTypeClass() );
					// todo (6.0) : is this the best approach?  or should we keep a Class<? extends UserType> -> CustomType mapping somewhere?
					resolvedBasicType = new CustomType<>( (UserType<Object>) userTypeBean.getBeanInstance(), sessionFactory.getTypeConfiguration() );
					this.explicitJavaTypeDescriptor = resolvedBasicType.getJavaTypeDescriptor();
				}
				else {
					this.explicitJavaTypeDescriptor = jtdRegistry.getDescriptor( definition.type() );
				}
			}
		}

		explicitType = resolvedBasicType;
	}

	public ResultMementoBasicStandard(
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
	public ResultBuilderBasicValued resolve(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		return new CompleteResultBuilderBasicValuedStandard( explicitColumnName, explicitType, explicitJavaTypeDescriptor );
	}
}
