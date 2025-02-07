/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.scope;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.util.Arrays;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.hibernate.processor.test.util.TestUtil.getMetamodelClassFor;

/**
 * @author Damiano Renfer
 */
public class ScopeTest extends CompilationTest {
	@Test
	@WithClasses({Thing.class, ThingRepo.class})
	public void testNoScopeAnnotation() {
		System.out.println( getMetaModelSourceAsString( ThingRepo.class ) );
		assertMetamodelClassGeneratedFor( Thing.class, true );
		assertMetamodelClassGeneratedFor( Thing.class );
		assertMetamodelClassGeneratedFor( ThingRepo.class );

		var repoClass = getMetamodelClassFor( ThingRepo.class );
		Assertions.assertTrue( Arrays.stream( repoClass.getAnnotations() ).anyMatch(
				a -> a.annotationType().getTypeName().equals( "jakarta.enterprise.context.RequestScoped" ) ) );
	}

	@Test
	@WithClasses({Thing.class, ApplicationScopedThingRepo.class})
	public void testApplicationScoped() {
		System.out.println( getMetaModelSourceAsString( ApplicationScopedThingRepo.class ) );
		assertMetamodelClassGeneratedFor( Thing.class, true );
		assertMetamodelClassGeneratedFor( Thing.class );
		assertMetamodelClassGeneratedFor( ApplicationScopedThingRepo.class );

		var repoClass = getMetamodelClassFor( ApplicationScopedThingRepo.class );
		Assertions.assertTrue( Arrays.stream( repoClass.getAnnotations() ).anyMatch(
				a -> a.annotationType().getTypeName().equals( "jakarta.enterprise.context.ApplicationScoped" ) ) );
	}

	@Test
	@WithClasses({Thing.class, DependentThingRepo.class})
	public void testDependent() {
		System.out.println( getMetaModelSourceAsString( DependentThingRepo.class ) );
		assertMetamodelClassGeneratedFor( Thing.class, true );
		assertMetamodelClassGeneratedFor( Thing.class );
		assertMetamodelClassGeneratedFor( DependentThingRepo.class );

		var repoClass = getMetamodelClassFor( DependentThingRepo.class );
		Assertions.assertTrue( Arrays.stream( repoClass.getAnnotations() )
				.anyMatch( a -> a.annotationType().getTypeName().equals( "jakarta.enterprise.context.Dependent" ) ) );
	}

	@Test
	@WithClasses({Thing.class, RequestScopedThingRepo.class})
	public void testRequestScoped() {
		System.out.println( getMetaModelSourceAsString( RequestScopedThingRepo.class ) );
		assertMetamodelClassGeneratedFor( Thing.class, true );
		assertMetamodelClassGeneratedFor( Thing.class );
		assertMetamodelClassGeneratedFor( RequestScopedThingRepo.class );

		var repoClass = getMetamodelClassFor( RequestScopedThingRepo.class );
		Assertions.assertTrue( Arrays.stream( repoClass.getAnnotations() ).anyMatch(
				a -> a.annotationType().getTypeName().equals( "jakarta.enterprise.context.RequestScoped" ) ) );
	}

	@Test
	@WithClasses({Thing.class, SessionScopedThingRepo.class})
	public void testSessionScoped() {
		System.out.println( getMetaModelSourceAsString( SessionScopedThingRepo.class ) );
		assertMetamodelClassGeneratedFor( Thing.class, true );
		assertMetamodelClassGeneratedFor( Thing.class );
		assertMetamodelClassGeneratedFor( SessionScopedThingRepo.class );

		var repoClass = getMetamodelClassFor( SessionScopedThingRepo.class );
		Assertions.assertTrue( Arrays.stream( repoClass.getAnnotations() ).anyMatch(
				a -> a.annotationType().getTypeName().equals( "jakarta.enterprise.context.SessionScoped" ) ) );
	}

	@Test
	@WithClasses({Thing.class, ConversationScopedThingRepo.class})
	public void testConversationScoped() {
		System.out.println( getMetaModelSourceAsString( ConversationScopedThingRepo.class ) );
		assertMetamodelClassGeneratedFor( Thing.class, true );
		assertMetamodelClassGeneratedFor( Thing.class );
		assertMetamodelClassGeneratedFor( ConversationScopedThingRepo.class );

		var repoClass = getMetamodelClassFor( ConversationScopedThingRepo.class );
		Assertions.assertTrue( Arrays.stream( repoClass.getAnnotations() ).anyMatch(
				a -> a.annotationType().getTypeName().equals( "jakarta.enterprise.context.ConversationScoped" ) ) );
	}

	@Test
	@WithClasses({Thing.class, TransactionScopedThingRepo.class})
	public void testTransactionScoped() {
		System.out.println( getMetaModelSourceAsString( TransactionScopedThingRepo.class ) );
		assertMetamodelClassGeneratedFor( Thing.class, true );
		assertMetamodelClassGeneratedFor( Thing.class );
		assertMetamodelClassGeneratedFor( TransactionScopedThingRepo.class );

		var repoClass = getMetamodelClassFor( TransactionScopedThingRepo.class );
		Assertions.assertTrue( Arrays.stream( repoClass.getAnnotations() ).anyMatch(
				a -> a.annotationType().getTypeName().equals( "jakarta.transaction.TransactionScoped" ) ) );
	}

	@Test
	@WithClasses({Thing.class, SingletonThingRepo.class})
	public void testSingleton() {
		System.out.println( getMetaModelSourceAsString( SingletonThingRepo.class ) );
		assertMetamodelClassGeneratedFor( Thing.class, true );
		assertMetamodelClassGeneratedFor( Thing.class );
		assertMetamodelClassGeneratedFor( SingletonThingRepo.class );

		var repoClass = getMetamodelClassFor( SingletonThingRepo.class );
		Assertions.assertTrue( Arrays.stream( repoClass.getAnnotations() ).anyMatch(
				a -> a.annotationType().getTypeName().equals( "jakarta.inject.Singleton" ) ) );
	}
}
