package org.hibernate.processor.test.data.stateful;



import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;
import org.hibernate.processor.test.util.CompilationDiagnostics;
import org.hibernate.processor.test.util.IgnoreCompilationErrors;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class StatefulRepositoryTest {
	@Test
	@WithClasses({ StatefulBook.class, StatefulBookRepository.class })
	void statefulRepositoryUsesStatefulSessionAndLifecycleOperations() {
		final String repository = getMetaModelSourceAsString( StatefulBookRepository.class, true );
		final String entityMetamodel = getMetaModelSourceAsString( StatefulBook.class, true );
		System.out.println( repository );
		assertMetamodelClassGeneratedFor( StatefulBook.class );
		assertMetamodelClassGeneratedFor( StatefulBook.class, true );
		assertMetamodelClassGeneratedFor( StatefulBookRepository.class, true );

		assertTrue( repository.contains( "protected @Nonnull EntityManager entityManager;" ) );
		assertTrue( repository.contains( "public @Nonnull EntityManager entityManager()" ) );
		assertFalse( repository.contains( "openStatelessSession()" ) );
		assertFalse( repository.contains( "@EntityListener" ) );
		assertFalse( repository.contains( "private Event<LifecycleEvent<?>> event;" ) );

		assertTrue( repository.contains( "entityManager.createQuery(_query)" ) );
		assertTrue( repository.contains( "entityManager.persist(book);" ) );
		assertTrue( repository.contains( "for (var _entity : books)" ) );
		assertTrue( repository.contains( "entityManager.persist(_entity);" ) );
		assertTrue( repository.contains( "book = entityManager.merge(book);" ) );
		assertTrue( repository.contains( "List<StatefulBook> _result;" ) );
		assertTrue( repository.contains( "_result = new ArrayList<>();" ) );
		assertTrue( repository.contains( "_result.add(entityManager.merge(_entity));" ) );
		assertTrue( repository.contains( "_result = books.clone();" ) );
		assertTrue( repository.contains( "_result[_index] = entityManager.merge(books[_index]);" ) );
		assertTrue( repository.contains( "entityManager.refresh(book);" ) );
		assertTrue( repository.contains( "entityManager.remove(book);" ) );
		assertTrue( repository.contains( "entityManager.detach(book);" ) );

		assertTrue( entityMetamodel.contains( "@EntityListener" ) );
		assertTrue( entityMetamodel.contains( "public class _StatefulBook" ) );
		assertTrue( entityMetamodel.contains( "private Event<LifecycleEvent<?>> event;" ) );
		assertTrue( entityMetamodel.contains( "@PreInsert" ) );
		assertTrue( entityMetamodel.contains( "void _onPreInsert(StatefulBook entity)" ) );
		assertTrue( entityMetamodel.contains( ".fire(new PreInsertEvent<>(entity));" ) );
		assertTrue( entityMetamodel.contains( "@PostInsert" ) );
		assertTrue( entityMetamodel.contains( "void _onPostInsert(StatefulBook entity)" ) );
		assertTrue( entityMetamodel.contains( ".fire(new PostInsertEvent<>(entity));" ) );
		assertTrue( entityMetamodel.contains( "@PreUpdate" ) );
		assertTrue( entityMetamodel.contains( "void _onPreUpdate(StatefulBook entity)" ) );
		assertTrue( entityMetamodel.contains( ".fire(new PreUpdateEvent<>(entity));" ) );
		assertTrue( entityMetamodel.contains( "@PostUpdate" ) );
		assertTrue( entityMetamodel.contains( "void _onPostUpdate(StatefulBook entity)" ) );
		assertTrue( entityMetamodel.contains( ".fire(new PostUpdateEvent<>(entity));" ) );
		assertTrue( entityMetamodel.contains( "@PreDelete" ) );
		assertTrue( entityMetamodel.contains( "void _onPreDelete(StatefulBook entity)" ) );
		assertTrue( entityMetamodel.contains( ".fire(new PreDeleteEvent<>(entity));" ) );
		assertTrue( entityMetamodel.contains( "@PostDelete" ) );
		assertTrue( entityMetamodel.contains( "void _onPostDelete(StatefulBook entity)" ) );
		assertTrue( entityMetamodel.contains( ".fire(new PostDeleteEvent<>(entity));" ) );
		assertTrue( entityMetamodel.contains( "@PreUpsert" ) );
		assertTrue( entityMetamodel.contains( "void _onPreUpsert(StatefulBook entity)" ) );
		assertTrue( entityMetamodel.contains( ".fire(new PreUpsertEvent<>(entity));" ) );
		assertTrue( entityMetamodel.contains( "@PostUpsert" ) );
		assertTrue( entityMetamodel.contains( "void _onPostUpsert(StatefulBook entity)" ) );
		assertTrue( entityMetamodel.contains( ".fire(new PostUpsertEvent<>(entity));" ) );
		assertFalse( repository.contains( "_hibernate_data_" ) );
	}

	@Test
	@WithClasses({ StatefulBook.class, EntityManagerStatefulBookRepository.class })
	void statefulRepositoryMayUseEntityManagerAccessor() {
		final String repository = getMetaModelSourceAsString( EntityManagerStatefulBookRepository.class, true );
		assertMetamodelClassGeneratedFor( EntityManagerStatefulBookRepository.class, true );

		assertTrue( repository.contains( "protected @Nonnull EntityManager entityManager;" ) );
		assertTrue( repository.contains( "public @Nonnull EntityManager entityManager()" ) );
		assertFalse( repository.contains( "StatelessSession" ) );
		assertFalse( repository.contains( "createSelectionQuery(_query)" ) );
		assertFalse( repository.contains( "@EntityListener" ) );

		assertTrue( repository.contains( "entityManager.createQuery(_query)" ) );
		assertTrue( repository.contains( "entityManager.persist(book);" ) );
		assertTrue( repository.contains( "book = entityManager.merge(book);" ) );
		assertTrue( repository.contains( "entityManager.refresh(book);" ) );
		assertTrue( repository.contains( "entityManager.remove(book);" ) );
		assertTrue( repository.contains( "entityManager.detach(book);" ) );
	}

	@Test
	@WithClasses({ StatefulBook.class, StatefulBookRepository.class, StatelessBookRepository.class })
	void defaultConstructorPathInjectsEntityManagerAndEntityAgent() {
		final String statefulRepository = getMetaModelSourceAsString( StatefulBookRepository.class, true );
		assertTrue( statefulRepository.contains( "@PersistenceContext" ) );
		assertTrue( statefulRepository.contains( "protected @Nonnull EntityManager entityManager;" ) );
		assertTrue( statefulRepository.contains( "@Inject\n\t_StatefulBookRepository()" ) );
		assertFalse( statefulRepository.contains( "EntityManagerFactory" ) );
		assertFalse( statefulRepository.contains( "createEntityManager()" ) );
		assertFalse( statefulRepository.contains( "entityManager.close();" ) );
		assertFalse( statefulRepository.contains( "@PostConstruct" ) );
		assertFalse( statefulRepository.contains( "@PreDestroy" ) );
		assertFalse( statefulRepository.contains( ".openStatelessSession();" ) );
		assertFalse( statefulRepository.contains( "@EntityListener" ) );
		assertFalse( statefulRepository.contains( ".fire(new PreInsertEvent<>(entity));" ) );
		assertFalse( statefulRepository.contains( ".fire(new PostDeleteEvent<>(entity));" ) );

		final String entityMetamodel = getMetaModelSourceAsString( StatefulBook.class, true );
		assertTrue( entityMetamodel.contains( "@EntityListener" ) );
		assertTrue( entityMetamodel.contains( ".fire(new PreInsertEvent<>(entity));" ) );
		assertTrue( entityMetamodel.contains( ".fire(new PostDeleteEvent<>(entity));" ) );

		final String statelessRepository = getMetaModelSourceAsString( StatelessBookRepository.class, true );
		assertTrue( statelessRepository.contains( "@PersistenceAgent" ) );
		assertTrue( statelessRepository.contains( "protected @Nonnull EntityAgent entityAgent;" ) );
		assertTrue( statelessRepository.contains( "@Inject\n\t_StatelessBookRepository()" ) );
		assertFalse( statelessRepository.contains( "EntityManagerFactory" ) );
		assertFalse( statelessRepository.contains( "createEntityAgent()" ) );
		assertFalse( statelessRepository.contains( "entityAgent.close();" ) );
		assertFalse( statelessRepository.contains( "@PostConstruct" ) );
		assertFalse( statelessRepository.contains( "@PreDestroy" ) );
		assertFalse( statelessRepository.contains( "SessionFactory.class).openSession();" ) );
		assertFalse( statelessRepository.contains( "@EntityListener" ) );
	}

	@Test
	@WithClasses({
			StatefulBook.class,
			InvalidStatefulBookRepository.class,
			InvalidStatelessBackedStatefulBookRepository.class
	})
	@IgnoreCompilationErrors
	void invalidStatefulRepositoryUsesMeaningfulDiagnostics(CompilationDiagnostics diagnostics) {
		diagnostics.assertError( "repository mixes stateful and stateless lifecycle annotations" );
		diagnostics.assertError(
				"repository with stateful lifecycle methods must be backed by a 'Session' or 'EntityManager'" );
		diagnostics.assertError( "method annotated '@Persist' must be declared 'void'" );
		diagnostics.assertError( "method annotated '@Merge' must have the same return type as its parameter" );
	}
}
