/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.procedure;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.internal.NamedCallableQueryMementoImpl;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.IntegerJavaType;
import org.hibernate.type.descriptor.java.LongJavaType;
import org.hibernate.type.descriptor.java.StringJavaType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public abstract class AbstractStoredProcedureTest {
	@Test
	public void testNamedStoredProcedureBinding(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			SessionFactoryImplementor sf = em.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
			final NamedCallableQueryMementoImpl m1 = (NamedCallableQueryMementoImpl) sf.getQueryEngine()
					.getNamedObjectRepository()
					.getCallableQueryMemento( "s1" );
			assertNotNull( m1 );
			assertEquals( "p1", m1.getCallableName() );
			assertEquals( ParameterStrategy.NAMED, m1.getParameterStrategy() );
			List<NamedCallableQueryMemento.ParameterMemento> list = m1.getParameterMementos();
			assertEquals( 2, list.size() );
			NamedCallableQueryMemento.ParameterMemento memento = list.get( 0 );
			ProcedureParameterImplementor parameterImplementor = memento.resolve( (SharedSessionContractImplementor) em );
			assertEquals( "p11", parameterImplementor.getName() );
			assertEquals( jakarta.persistence.ParameterMode.IN, parameterImplementor.getMode() );
			BasicType hibernateType = (BasicType) parameterImplementor.getHibernateType();
			assertEquals( IntegerJavaType.INSTANCE, hibernateType.getJavaTypeDescriptor() );
			assertEquals( Integer.class, parameterImplementor.getParameterType() );

			memento = list.get( 1 );
			parameterImplementor = memento.resolve( (SharedSessionContractImplementor) em );
			assertEquals( "p12", parameterImplementor.getName() );
			assertEquals( jakarta.persistence.ParameterMode.IN, parameterImplementor.getMode() );
			hibernateType = (BasicType) parameterImplementor.getHibernateType();
			assertEquals( IntegerJavaType.INSTANCE, hibernateType.getJavaTypeDescriptor() );
			assertEquals( Integer.class, parameterImplementor.getParameterType() );

			final NamedCallableQueryMementoImpl m2 = (NamedCallableQueryMementoImpl) sf.getQueryEngine()
					.getNamedObjectRepository()
					.getCallableQueryMemento( "s2" );
			assertNotNull( m2 );
			assertEquals( "p2", m2.getCallableName() );
			assertEquals( ParameterStrategy.POSITIONAL, m2.getParameterStrategy() );
			list = m2.getParameterMementos();

			memento = list.get( 0 );
			parameterImplementor = memento.resolve( (SharedSessionContractImplementor) em );
			assertEquals( Integer.valueOf( 1 ), parameterImplementor.getPosition() );
			assertEquals( jakarta.persistence.ParameterMode.INOUT, parameterImplementor.getMode() );
			hibernateType = (BasicType) parameterImplementor.getHibernateType();

			assertEquals( StringJavaType.INSTANCE, hibernateType.getJavaTypeDescriptor() );
			assertEquals( String.class, parameterImplementor.getParameterType() );

			memento = list.get( 1 );
			parameterImplementor = memento.resolve( (SharedSessionContractImplementor) em );

			assertEquals( Integer.valueOf( 2 ), parameterImplementor.getPosition() );
			assertEquals( jakarta.persistence.ParameterMode.INOUT, parameterImplementor.getMode() );
			hibernateType = (BasicType) parameterImplementor.getHibernateType();

			assertEquals( LongJavaType.INSTANCE, hibernateType.getJavaTypeDescriptor() );
			assertEquals( Long.class, parameterImplementor.getParameterType() );
		} );

	}
}
