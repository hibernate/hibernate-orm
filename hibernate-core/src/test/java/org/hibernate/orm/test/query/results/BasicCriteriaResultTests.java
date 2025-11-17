/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.results;

import java.util.List;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = {SimpleEntity.class, Dto.class, Dto2.class } )
@SessionFactory
public class BasicCriteriaResultTests {
	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new SimpleEntity( 1, "first", new SimpleComposite( "a", "b" ) ) );
		});
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testBasicSelection(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final CriteriaBuilder builder = scope.getSessionFactory().getCriteriaBuilder();
			final JpaMetamodel jpaMetamodel = scope.getSessionFactory().getJpaMetamodel();
			final EntityDomainType<SimpleEntity> entityDescriptor = jpaMetamodel.entity( SimpleEntity.class );
			final SingularPersistentAttribute<? super SimpleEntity, Integer> idAttribute = entityDescriptor.getId( Integer.class );

			final CriteriaQuery<Integer> criteria = builder.createQuery( Integer.class );
			final Root<SimpleEntity> root = criteria.from( SimpleEntity.class );

//			final Path<Integer> idPath = root.get( SimpleEntity_ );
			final Path<Integer> idPath = root.get( idAttribute );
			criteria.select( idPath );
			criteria.orderBy( builder.asc( idPath ) );

			session.createQuery( criteria ).list();
		});
	}

	@Test
	public void testBasicStatelessSelection(SessionFactoryScope scope) {
		scope.inStatelessTransaction( (session) -> {
			final CriteriaBuilder builder = scope.getSessionFactory().getCriteriaBuilder();
			final JpaMetamodel jpaMetamodel = scope.getSessionFactory().getJpaMetamodel();
			final EntityDomainType<SimpleEntity> entityDescriptor = jpaMetamodel.entity( SimpleEntity.class );
			final SingularPersistentAttribute<? super SimpleEntity, Integer> idAttribute = entityDescriptor.getId( Integer.class );

			final CriteriaQuery<Integer> criteria = builder.createQuery( Integer.class );
			final Root<SimpleEntity> root = criteria.from( SimpleEntity.class );

//			final Path<Integer> idPath = root.get( SimpleEntity_ );
			final Path<Integer> idPath = root.get( idAttribute );
			criteria.select( idPath );
			criteria.orderBy( builder.asc( idPath ) );

			session.createQuery( criteria ).list();
		});
	}

	@Test
	public void testBasicStatelessSelectionMixedPathRefs(SessionFactoryScope scope) {
		scope.inStatelessTransaction( (session) -> {
			final CriteriaBuilder builder = scope.getSessionFactory().getCriteriaBuilder();
			final JpaMetamodel jpaMetamodel = scope.getSessionFactory().getJpaMetamodel();
			final EntityDomainType<SimpleEntity> entityDescriptor = jpaMetamodel.entity( SimpleEntity.class );
			final Class<Integer> idType = (Class<Integer>) entityDescriptor.getIdType().getJavaType();

			final CriteriaQuery<Integer> criteria = builder.createQuery( Integer.class );
			final Root<SimpleEntity> root = criteria.from( SimpleEntity.class );

			criteria.select( root.get( entityDescriptor.getId( idType ) ) );
			criteria.orderBy( builder.asc( root.get( "id" ) ) );

			session.createQuery( criteria ).list();
		});
	}

	@Test
	public void testBasicTupleSelection(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final CriteriaBuilder builder = scope.getSessionFactory().getCriteriaBuilder();
			final JpaMetamodel jpaMetamodel = scope.getSessionFactory().getJpaMetamodel();
			final EntityDomainType<SimpleEntity> entityDescriptor = jpaMetamodel.entity( SimpleEntity.class );
			final SingularAttribute<? super SimpleEntity, Integer> idAttribute = entityDescriptor.getId( Integer.class );
			final SingularAttribute<? super SimpleEntity, String> nameAttribute = entityDescriptor.getSingularAttribute( "name", String.class );

			final CriteriaQuery<Tuple> criteria = builder.createQuery( Tuple.class );
			final Root<SimpleEntity> root = criteria.from( SimpleEntity.class );

			final Path<Integer> idPath = root.get( idAttribute );
			final Path<String> namePath = root.get( nameAttribute );
			criteria.multiselect( idPath.alias( "id" ), namePath.alias( "name" ) );
			criteria.orderBy( builder.asc( idPath ), builder.asc( namePath ) );

			final List<Tuple> list = session.createQuery( criteria ).list();
			assertThat( list ).hasSize( 1 );

			final Tuple result = list.get( 0 );
			assertThat( result.get( 0 ) ).isEqualTo( 1 );
			assertThat( result.get( "id" ) ).isEqualTo( 1 );

			assertThat( result.get( 1 ) ).isEqualTo( "first" );
			assertThat( result.get( "name" ) ).isEqualTo( "first" );
		});
	}

	@Test
	public void testCompositeSelection(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final CriteriaBuilder builder = scope.getSessionFactory().getCriteriaBuilder();
			final JpaMetamodel jpaMetamodel = scope.getSessionFactory().getJpaMetamodel();
			final EntityDomainType<SimpleEntity> entityDescriptor = jpaMetamodel.entity( SimpleEntity.class );
			final SingularAttribute<? super SimpleEntity, SimpleComposite> compositeAttribute;
			compositeAttribute = entityDescriptor.getSingularAttribute( "composite", SimpleComposite.class );

			final CriteriaQuery<SimpleComposite> criteria = builder.createQuery( SimpleComposite.class );
			final Root<SimpleEntity> root = criteria.from( SimpleEntity.class );

			final Path<SimpleComposite> attributePath = root.get( compositeAttribute );
			criteria.select( attributePath );
			criteria.orderBy( builder.asc( attributePath ) );

			session.createQuery( criteria ).list();
		});
	}

	@Test
	public void testBasicAndCompositeTuple(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final CriteriaBuilder builder = scope.getSessionFactory().getCriteriaBuilder();
			final JpaMetamodel jpaMetamodel = scope.getSessionFactory().getJpaMetamodel();
			final EntityDomainType<SimpleEntity> entityDescriptor = jpaMetamodel.entity( SimpleEntity.class );
			final SingularAttribute<? super SimpleEntity, Integer> idAttribute = entityDescriptor.getId( Integer.class );
			final SingularAttribute<? super SimpleEntity, SimpleComposite> compositeAttribute = entityDescriptor.getSingularAttribute( "composite", SimpleComposite.class );

			final CriteriaQuery<Tuple> criteria = builder.createQuery( Tuple.class );
			final Root<SimpleEntity> root = criteria.from( SimpleEntity.class );

			final Path<Integer> idPath = root.get( idAttribute );
			final Path<SimpleComposite> compositePath = root.get( compositeAttribute );
			criteria.multiselect( idPath.alias( "id" ), compositePath.alias( "composite" ) );
			criteria.orderBy( builder.asc( idPath ) );

			final List<Tuple> list = session.createQuery( criteria ).list();
			assertThat( list ).hasSize( 1 );

			final Tuple result = list.get( 0 );
			assertThat( result.get( 0 ) ).isEqualTo( 1 );
			assertThat( result.get( "id" ) ).isEqualTo( 1 );
			assertThat( result.get( 0 ) ).isSameAs( result.get( "id" ) );

			assertThat( result.get( 1 ) ).isInstanceOf( SimpleComposite.class );
			assertThat( result.get( 1 ) ).isSameAs( result.get( "composite" ) );
		});
	}

	@Test
	public void testBasicAndCompositeArray(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final CriteriaBuilder builder = scope.getSessionFactory().getCriteriaBuilder();
			final JpaMetamodel jpaMetamodel = scope.getSessionFactory().getJpaMetamodel();
			final EntityDomainType<SimpleEntity> entityDescriptor = jpaMetamodel.entity( SimpleEntity.class );
			final SingularAttribute<? super SimpleEntity, Integer> idAttribute = entityDescriptor.getId( Integer.class );
			final SingularAttribute<? super SimpleEntity, SimpleComposite> compositeAttribute = entityDescriptor.getSingularAttribute( "composite", SimpleComposite.class );

			final CriteriaQuery<Object[]> criteria = builder.createQuery( Object[].class );
			final Root<SimpleEntity> root = criteria.from( SimpleEntity.class );

			final Path<Integer> idPath = root.get( idAttribute );
			final Path<SimpleComposite> compositePath = root.get( compositeAttribute );
			criteria.multiselect( idPath.alias( "id" ), compositePath.alias( "composite" ) );
			criteria.orderBy( builder.asc( idPath ) );

			final List<Object[]> list = session.createQuery( criteria ).list();
			assertThat( list ).hasSize( 1 );

			final Object[] result = list.get( 0 );
			assertThat( result[0] ).isEqualTo( 1 );
			assertThat( result[1] ).isInstanceOf( SimpleComposite.class );
		});
	}

}
