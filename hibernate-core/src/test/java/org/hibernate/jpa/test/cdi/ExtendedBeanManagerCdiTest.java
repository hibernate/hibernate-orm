/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProducerFactory;
import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import org.hibernate.jpa.event.spi.jpa.ExtendedBeanManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class ExtendedBeanManagerCdiTest extends BaseCdiIntegrationTest {
	ExtendedBeanManagerImpl extendedBeanManager = new ExtendedBeanManagerImpl();

	private static int count;

	@Override
	public Class[] getCdiBeans() {
		return new Class[] { EventQueue.class };
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class };
	}

	@Override
	protected BeanManager getBeanManager() {
		// this is the BeanManager used to bootstrap Hibernate.  Here
		// we want this to be our custom ExtendedBeanManagerImpl
		return extendedBeanManager;
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		// now come back and do the ExtendedBeanManagerImpl lifecycle callbacks
		final BeanManager realBeanManager = getTestContainer().getBeanManager( getTestContainer().getDeployment().getBeanDeploymentArchives().iterator().next() );
		for ( ExtendedBeanManager.LifecycleListener lifecycleListener : extendedBeanManager.getLifecycleListeners() ) {
			lifecycleListener.beanManagerInitialized( realBeanManager );
		}
		extendedBeanManager.getLifecycleListeners().clear();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testIt() {
		count = 0;

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( new MyEntity( 1 ) );
		em.getTransaction().commit();
		em.close();

		assertEquals( 1, count );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.remove( em.getReference( MyEntity.class, 1 ) );
		em.getTransaction().commit();
		em.close();
	}

	@Entity
	@EntityListeners( Monitor.class )
	@Table(name = "my_entity")
	public static class MyEntity {
		private Integer id;
		private String name;

		public MyEntity() {
		}

		public MyEntity(Integer id) {
			this.id = id;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class EventQueue {
		private List<Event> events;

		public void addEvent(Event anEvent) {
			if ( events == null ) {
				events = new ArrayList<Event>();
			}
			events.add( anEvent );
			count++;
		}
	}

	public static class Event {
		private final String who;
		private final String what;
		private final String when;

		public Event(String who, String what, String when) {
			this.who = who;
			this.what = what;
			this.when = when;
		}

		public String getWho() {
			return who;
		}

		public String getWhat() {
			return what;
		}

		public String getWhen() {
			return when;
		}
	}

	public static class Monitor {
		private final EventQueue eventQueue;

		@Inject
		public Monitor(EventQueue eventQueue) {
			this.eventQueue = eventQueue;
		}

		@PrePersist
		public void onCreate(Object entity) {
			eventQueue.addEvent(
					new Event( entity.toString(), "created", now() )
			);
		}

		private String now() {
			return new SimpleDateFormat().format( new Date() );
		}
	}

	public class ExtendedBeanManagerImpl implements BeanManager, ExtendedBeanManager {
		private List<LifecycleListener> lifecycleListeners = new ArrayList<LifecycleListener>();

		@Override
		public void registerLifecycleListener(LifecycleListener lifecycleListener) {
			lifecycleListeners.add( lifecycleListener );
		}

		public List<LifecycleListener> getLifecycleListeners() {
			return lifecycleListeners;
		}


		// ~~~~

		@Override
		public Object getReference(Bean<?> bean, Type beanType, CreationalContext<?> ctx) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public Object getInjectableReference(InjectionPoint ij, CreationalContext<?> ctx) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public <T> CreationalContext<T> createCreationalContext(Contextual<T> contextual) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public Set<Bean<?>> getBeans(Type beanType, Annotation... qualifiers) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public Set<Bean<?>> getBeans(String name) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public Bean<?> getPassivationCapableBean(String id) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public <X> Bean<? extends X> resolve(Set<Bean<? extends X>> beans) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public void validate(InjectionPoint injectionPoint) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public void fireEvent(Object event, Annotation... qualifiers) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public <T> Set<ObserverMethod<? super T>> resolveObserverMethods(T event, Annotation... qualifiers) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public List<Decorator<?>> resolveDecorators(Set<Type> types, Annotation... qualifiers) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public List<Interceptor<?>> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public boolean isScope(Class<? extends Annotation> annotationType) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public boolean isNormalScope(Class<? extends Annotation> annotationType) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public boolean isPassivatingScope(Class<? extends Annotation> annotationType) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public boolean isQualifier(Class<? extends Annotation> annotationType) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public boolean isInterceptorBinding(Class<? extends Annotation> annotationType) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public boolean isStereotype(Class<? extends Annotation> annotationType) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public Set<Annotation> getInterceptorBindingDefinition(Class<? extends Annotation> bindingType) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public Set<Annotation> getStereotypeDefinition(Class<? extends Annotation> stereotype) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public boolean areQualifiersEquivalent(Annotation qualifier1, Annotation qualifier2) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public boolean areInterceptorBindingsEquivalent(Annotation interceptorBinding1, Annotation interceptorBinding2) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public int getQualifierHashCode(Annotation qualifier) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public int getInterceptorBindingHashCode(Annotation interceptorBinding) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public Context getContext(Class<? extends Annotation> scopeType) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public ELResolver getELResolver() {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public ExpressionFactory wrapExpressionFactory(ExpressionFactory expressionFactory) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public <T> AnnotatedType<T> createAnnotatedType(Class<T> type) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public <T> InjectionTarget<T> createInjectionTarget(AnnotatedType<T> type) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public <T> InjectionTargetFactory<T> getInjectionTargetFactory(AnnotatedType<T> annotatedType) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public <X> ProducerFactory<X> getProducerFactory(AnnotatedField<? super X> field) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public <X> ProducerFactory<X> getProducerFactory(AnnotatedMethod<? super X> method) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public <T> BeanAttributes<T> createBeanAttributes(AnnotatedType<T> type) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public BeanAttributes<?> createBeanAttributes(AnnotatedMember<?> type) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public <T> Bean<T> createBean(BeanAttributes<T> attributes, Class<T> beanClass, InjectionTargetFactory<T> injectionTargetFactory) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public <T> Bean<T> createBean(BeanAttributes<T> attributes, Class<?> beanClass, ProducerFactory<T> producerFactory) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public InjectionPoint createInjectionPoint(AnnotatedField<?> field) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public InjectionPoint createInjectionPoint(AnnotatedParameter<?> parameter) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}

		@Override
		public <T extends Extension> T getExtension(Class<T> extensionClass) {
			throw new UnsupportedOperationException( "ExtendedBeanManagerImpl here just to gainn access to BeanManager lazily" );
		}
	}
}
