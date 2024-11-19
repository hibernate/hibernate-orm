/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class CompositeIdAttributeAccessTests {
	@Test
	@DomainModel(annotatedClasses = {AnEntity.PK.class, AnEntity.class, AnotherEntity.class})
	@SessionFactory
	void testSubAttributeAccess(SessionFactoryScope sessions) {
		final EntityManagerFactory sessionFactory = (EntityManagerFactory) sessions.getSessionFactory();
		final EntityType<AnEntity> entityType = sessionFactory.getMetamodel().entity( AnEntity.class );
		checkEntityType( entityType );

		final EmbeddableType<AnEntity.PK> idClassType = sessionFactory.getMetamodel().embeddable( AnEntity.PK.class );
		checkIdClassType( idClassType );
	}

	private void checkEntityType(EntityType<AnEntity> entityType) {
		final SingularAttribute<? super AnEntity, ?> key1Attribute = entityType.getSingularAttribute( "key1" );
		assertThat( key1Attribute.isId() ).isTrue();
		assertThat( key1Attribute.getJavaType() ).isEqualTo( Integer.class );
		assertThat( key1Attribute.getJavaMember().getDeclaringClass() ).isEqualTo( AnEntity.class );

		final SingularAttribute<? super AnEntity, ?> key2Attribute = entityType.getSingularAttribute( "key2" );
		assertThat( key2Attribute.isId() ).isTrue();
		assertThat( key2Attribute.getJavaType() ).isEqualTo( AnotherEntity.class );
		assertThat( key2Attribute.getJavaMember().getDeclaringClass() ).isEqualTo( AnEntity.class );

		assertThat( entityType.getIdClassAttributes() ).hasSize( 2 );
		for ( SingularAttribute<? super AnEntity, ?> idClassAttribute : entityType.getIdClassAttributes() ) {
			if ( "key1".equals( idClassAttribute.getName() ) )  {
				assertThat( idClassAttribute ).isSameAs( key1Attribute );
			}
			else if ( "key2".equals( idClassAttribute.getName() ) ) {
				assertThat( idClassAttribute ).isSameAs( key2Attribute );
			}
			else {
				fail( "Unexpected attribute : " + idClassAttribute );
			}
		}
	}

	private void checkIdClassType(EmbeddableType<AnEntity.PK> idClassType) {
		assertThat( idClassType.getAttributes() ).hasSize( 2 );

		final SingularAttribute<? super AnEntity.PK, ?> key1Attribute = idClassType.getSingularAttribute( "key1" );
		// we don't know this at the embeddable level
		assertThat( key1Attribute.isId() ).isFalse();
		assertThat( key1Attribute.getJavaType() ).isEqualTo( Integer.class );
		assertThat( key1Attribute.getJavaMember().getDeclaringClass() ).isEqualTo( AnEntity.PK.class );

		final SingularAttribute<? super AnEntity.PK, ?> key2Attribute = idClassType.getSingularAttribute( "key2" );
		// we don't know this at the embeddable level
		assertThat( key2Attribute.isId() ).isFalse();
		assertThat( key2Attribute.getJavaType() ).isEqualTo( Integer.class );
		assertThat( key2Attribute.getJavaMember().getDeclaringClass() ).isEqualTo( AnEntity.PK.class );
	}

	@Entity(name="AnEntity")
	@Table(name="entity_a")
	@IdClass(AnEntity.PK.class)
	public static class AnEntity {
		@Id
		private Integer key1;
		@Id @ManyToOne
		private AnotherEntity key2;

		private String name;

		public static class PK implements Serializable {
			private Integer key1;
			private Integer key2;
		}
	}

	@Entity(name="AnotherEntity")
	@Table(name="entity_b")
	public static class AnotherEntity {
		@Id
		private Integer id;
		private String name;
	}
}
