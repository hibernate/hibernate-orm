/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.pojo.ProxyFactoryHelper;
import org.hibernate.type.CompositeType;

/**
 * An {@link EntityTuplizer} specific to the pojo entity mode.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class PojoEntityTuplizer extends AbstractEntityTuplizer {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( PojoEntityTuplizer.class );

	private final Class<?> mappedClass;
	private final Class<?> proxyInterface;
	private final ReflectionOptimizer optimizer;

	public PojoEntityTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappedEntity) {
		super( entityMetamodel, mappedEntity );
		this.mappedClass = mappedEntity.getMappedClass();
		this.proxyInterface = mappedEntity.getProxyInterface();

		String[] getterNames = new String[propertySpan];
		String[] setterNames = new String[propertySpan];
		Class<?>[] propTypes = new Class[propertySpan];
		for ( int i = 0; i < propertySpan; i++ ) {
			getterNames[i] = getters[i].getMethodName();
			setterNames[i] = setters[i].getMethodName();
			propTypes[i] = getters[i].getReturnTypeClass();
		}

		if ( hasCustomAccessors || !Environment.useReflectionOptimizer() ) {
			optimizer = null;
		}
		else {
			final BytecodeProvider bytecodeProvider = entityMetamodel.getSessionFactory().getServiceRegistry().getService( BytecodeProvider.class );
			optimizer = bytecodeProvider.getReflectionOptimizer(
					mappedClass,
					getterNames,
					setterNames,
					propTypes
			);
		}
	}

	@Override
	protected ProxyFactory buildProxyFactory(PersistentClass persistentClass, Getter idGetter, Setter idSetter) {
		// determine the id getter and setter methods from the proxy interface (if any)
		// determine all interfaces needed by the resulting proxy
		final String entityName = getEntityName();
		final Class<?> mappedClass = persistentClass.getMappedClass();
		final Class<?> proxyInterface = persistentClass.getProxyInterface();

		final Set<Class<?>> proxyInterfaces = ProxyFactoryHelper.extractProxyInterfaces( persistentClass, entityName );

		Method proxyGetIdentifierMethod = ProxyFactoryHelper.extractProxyGetIdentifierMethod( idGetter, proxyInterface );
		Method proxySetIdentifierMethod = ProxyFactoryHelper.extractProxySetIdentifierMethod( idSetter, proxyInterface );

		ProxyFactory pf = buildProxyFactoryInternal( persistentClass, idGetter, idSetter );
		try {

			ProxyFactoryHelper.validateGetterSetterMethodProxyability( "Getter", proxyGetIdentifierMethod );
			ProxyFactoryHelper.validateGetterSetterMethodProxyability( "Setter", proxySetIdentifierMethod );

			ProxyFactoryHelper.validateProxyability( persistentClass );

			pf.postInstantiate(
					entityName,
					mappedClass,
					proxyInterfaces,
					proxyGetIdentifierMethod,
					proxySetIdentifierMethod,
					persistentClass.hasEmbeddedIdentifier() ?
							(CompositeType) persistentClass.getIdentifier().getType() :
							null
			);
		}
		catch (HibernateException he) {
			LOG.unableToCreateProxyFactory( entityName, he );
			pf = null;
		}
		return pf;
	}

	protected ProxyFactory buildProxyFactoryInternal(
			PersistentClass persistentClass,
			Getter idGetter,
			Setter idSetter) {
		ProxyFactoryFactory proxyFactory = getFactory().getServiceRegistry().getService( ProxyFactoryFactory.class );
		return proxyFactory.buildProxyFactory( getFactory() );
	}

	@Override
	public Object[] getPropertyValuesToInsert(Object entity, Map mergeMap, SharedSessionContractImplementor session) {
		if ( shouldGetAllProperties( entity ) && optimizer != null && optimizer.getAccessOptimizer() != null ) {
			return getPropertyValuesWithOptimizer( entity );
		}
		else {
			return super.getPropertyValuesToInsert( entity, mergeMap, session );
		}
	}

	protected Object[] getPropertyValuesWithOptimizer(Object object) {
		return optimizer.getAccessOptimizer().getPropertyValues( object );
	}

	@Override
	public Class<?> getMappedClass() {
		return mappedClass;
	}

	@Override
	protected Getter buildPropertyGetter(Property mappedProperty, PersistentClass mappedEntity) {
		return mappedProperty.getGetter( mappedEntity.getMappedClass() );
	}

	@Override
	protected Setter buildPropertySetter(Property mappedProperty, PersistentClass mappedEntity) {
		return mappedProperty.getSetter( mappedEntity.getMappedClass() );
	}

	//TODO: need to make the majority of this functionality into a top-level support class for custom impl support

	@Override
	public String determineConcreteSubclassEntityName(Object entityInstance, SessionFactoryImplementor factory) {
		if ( entityInstance == null ) {
			return getEntityName();
		}

		final Class<?> concreteEntityClass = entityInstance.getClass();
		if ( concreteEntityClass == getMappedClass() ) {
			return getEntityName();
		}
		else {
			String entityName = getEntityMetamodel().findEntityNameByEntityClass( concreteEntityClass );
			if ( entityName == null ) {
				throw new HibernateException(
						"Unable to resolve entity name from Class [" + concreteEntityClass.getName() + "]"
								+ " expected instance/subclass of [" + getEntityName() + "]"
				);
			}
			return entityName;
		}
	}

	@Override
	public EntityNameResolver[] getEntityNameResolvers() {
		// the fallback is to check class name which is exactly what we'd do here
		return null;
	}
}
