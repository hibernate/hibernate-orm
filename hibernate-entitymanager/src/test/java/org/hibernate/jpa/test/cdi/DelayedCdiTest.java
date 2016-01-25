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
import java.util.Map;
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

import org.hibernate.jpa.AvailableSettings;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class DelayedCdiTest extends BaseCdiIntegrationTest {
	private BeanManagerImpl beanManager = new BeanManagerImpl();

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
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.DELAY_CDI_ACCESS, "true" );
		super.addConfigOptions( options );
	}

	@Override
	protected BeanManager getBeanManager() {
		// this is the BeanManager used to bootstrap Hibernate.  Here
		// we want this to be our custom BeanManagerImpl
		return beanManager;
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		beanManager.delegate = getTestContainer().getBeanManager( getTestContainer().getDeployment().getBeanDeploymentArchives().iterator().next() );
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

	public static class BeanManagerImpl implements BeanManager {
		private BeanManager delegate;

		@Override
		public Object getReference(Bean<?> bean, Type beanType, CreationalContext<?> ctx) {
			return getDelegate().getReference( bean, beanType, ctx );
		}

		private BeanManager getDelegate() {
			if ( delegate == null ) {
				throw new RuntimeException( "Real BeanManager not yet known" );
			}

			return delegate;
		}

		@Override
		public Object getInjectableReference(InjectionPoint ij, CreationalContext<?> ctx) {
			return getDelegate().getInjectableReference( ij, ctx );
		}

		@Override
		public <T> CreationalContext<T> createCreationalContext(Contextual<T> contextual) {
			return getDelegate().createCreationalContext( contextual );
		}

		@Override
		public Set<Bean<?>> getBeans(Type beanType, Annotation... qualifiers) {
			return getDelegate().getBeans( beanType, qualifiers );
		}

		@Override
		public Set<Bean<?>> getBeans(String name) {
			return getDelegate().getBeans( name );
		}

		@Override
		public Bean<?> getPassivationCapableBean(String id) {
			return getDelegate().getPassivationCapableBean( id );
		}

		@Override
		public <X> Bean<? extends X> resolve(Set<Bean<? extends X>> beans) {
			return getDelegate().resolve( beans );
		}

		@Override
		public void validate(InjectionPoint injectionPoint) {
			getDelegate().validate( injectionPoint );
		}

		@Override
		public void fireEvent(Object event, Annotation... qualifiers) {
			getDelegate().fireEvent( event, qualifiers );
		}

		@Override
		public <T> Set<ObserverMethod<? super T>> resolveObserverMethods(T event, Annotation... qualifiers) {
			return getDelegate().resolveObserverMethods( event, qualifiers );
		}

		@Override
		public List<Decorator<?>> resolveDecorators(Set<Type> types, Annotation... qualifiers) {
			return getDelegate().resolveDecorators( types, qualifiers );
		}

		@Override
		public List<Interceptor<?>> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings) {
			return getDelegate().resolveInterceptors( type, interceptorBindings );
		}

		@Override
		public boolean isScope(Class<? extends Annotation> annotationType) {
			return getDelegate().isScope( annotationType );
		}

		@Override
		public boolean isNormalScope(Class<? extends Annotation> annotationType) {
			return getDelegate().isNormalScope( annotationType );
		}

		@Override
		public boolean isPassivatingScope(Class<? extends Annotation> annotationType) {
			return getDelegate().isPassivatingScope( annotationType );
		}

		@Override
		public boolean isQualifier(Class<? extends Annotation> annotationType) {
			return getDelegate().isQualifier( annotationType );
		}

		@Override
		public boolean isInterceptorBinding(Class<? extends Annotation> annotationType) {
			return getDelegate().isInterceptorBinding( annotationType );
		}

		@Override
		public boolean isStereotype(Class<? extends Annotation> annotationType) {
			return getDelegate().isStereotype( annotationType );
		}

		@Override
		public Set<Annotation> getInterceptorBindingDefinition(Class<? extends Annotation> bindingType) {
			return getDelegate().getInterceptorBindingDefinition( bindingType );
		}

		@Override
		public Set<Annotation> getStereotypeDefinition(Class<? extends Annotation> stereotype) {
			return getDelegate().getStereotypeDefinition( stereotype );
		}

		@Override
		public boolean areQualifiersEquivalent(Annotation qualifier1, Annotation qualifier2) {
			return getDelegate().areQualifiersEquivalent( qualifier1, qualifier2 );
		}

		@Override
		public boolean areInterceptorBindingsEquivalent(Annotation interceptorBinding1, Annotation interceptorBinding2) {
			return getDelegate().areInterceptorBindingsEquivalent( interceptorBinding1, interceptorBinding2 );
		}

		@Override
		public int getQualifierHashCode(Annotation qualifier) {
			return getDelegate().getQualifierHashCode( qualifier );
		}

		@Override
		public int getInterceptorBindingHashCode(Annotation interceptorBinding) {
			return getDelegate().getInterceptorBindingHashCode( interceptorBinding );
		}

		@Override
		public Context getContext(Class<? extends Annotation> scopeType) {
			return getDelegate().getContext( scopeType );
		}

		@Override
		public ELResolver getELResolver() {
			return getDelegate().getELResolver();
		}

		@Override
		public ExpressionFactory wrapExpressionFactory(ExpressionFactory expressionFactory) {
			return getDelegate().wrapExpressionFactory( expressionFactory );
		}

		@Override
		public <T> AnnotatedType<T> createAnnotatedType(Class<T> type) {
			return getDelegate().createAnnotatedType( type );
		}

		@Override
		public <T> InjectionTarget<T> createInjectionTarget(AnnotatedType<T> type) {
			return getDelegate().createInjectionTarget( type );
		}

		@Override
		public <T> InjectionTargetFactory<T> getInjectionTargetFactory(AnnotatedType<T> annotatedType) {
			return getDelegate().getInjectionTargetFactory( annotatedType );
		}

		@Override
		public <X> ProducerFactory<X> getProducerFactory(AnnotatedField<? super X> field) {
			return getDelegate().getProducerFactory( field );
		}

		@Override
		public <X> ProducerFactory<X> getProducerFactory(AnnotatedMethod<? super X> method) {
			return getDelegate().getProducerFactory( method );
		}

		@Override
		public <T> BeanAttributes<T> createBeanAttributes(AnnotatedType<T> type) {
			return getDelegate().createBeanAttributes( type );
		}

		@Override
		public BeanAttributes<?> createBeanAttributes(AnnotatedMember<?> type) {
			return getDelegate().createBeanAttributes( type );
		}

		@Override
		public <T> Bean<T> createBean(BeanAttributes<T> attributes, Class<T> beanClass, InjectionTargetFactory<T> injectionTargetFactory) {
			return getDelegate().createBean( attributes, beanClass, injectionTargetFactory );
		}

		@Override
		public <T> Bean<T> createBean(BeanAttributes<T> attributes, Class<?> beanClass, ProducerFactory<T> producerFactory) {
			return getDelegate().createBean( attributes, beanClass, producerFactory );
		}

		@Override
		public InjectionPoint createInjectionPoint(AnnotatedField<?> field) {
			return getDelegate().createInjectionPoint( field );
		}

		@Override
		public InjectionPoint createInjectionPoint(AnnotatedParameter<?> parameter) {
			return getDelegate().createInjectionPoint( parameter );
		}

		@Override
		public <T extends Extension> T getExtension(Class<T> extensionClass) {
			return getDelegate().getExtension( extensionClass );
		}
	}
}
