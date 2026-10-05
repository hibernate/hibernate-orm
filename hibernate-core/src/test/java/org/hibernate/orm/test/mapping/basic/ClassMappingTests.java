package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = ClassMappingTests.EntityWithClass.class)
@SessionFactory
public class ClassMappingTests {

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey("HHH-10735")
	void testCriteriaComparisonWithMappedClass(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new EntityWithClass( 1, EntityWithClass.class ) );
			session.persist( new EntityWithClass( 2, String.class ) );
		} );

		scope.inTransaction( session -> {
			var builder = session.getCriteriaBuilder();
			var criteria = builder.createQuery( EntityWithClass.class );
			var root = criteria.from( EntityWithClass.class );
			var parameter = builder.parameter( Class.class, "clazz" );
			criteria.select( root ).where( builder.equal( root.get( "clazz" ), parameter ) );
			var results = session.createQuery( criteria )
					.setParameter( "clazz", EntityWithClass.class )
					.getResultList();
			assertThat( results.size(), equalTo( 1 ) );
			assertThat( results.get( 0 ).id, equalTo( 1 ) );
			assertThat( results.get( 0 ).clazz, equalTo( EntityWithClass.class ) );

			var literalCriteria = builder.createQuery( EntityWithClass.class );
			var literalRoot = literalCriteria.from( EntityWithClass.class );
			literalCriteria.select( literalRoot ).where(
					builder.equal( literalRoot.get( "clazz" ), EntityWithClass.class )
			);
			var result = session.createQuery( literalCriteria ).getSingleResult();
			assertThat( result.id, equalTo( 1 ) );
			assertThat( result.clazz, equalTo( EntityWithClass.class ) );
		} );
	}

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final JdbcTypeRegistry jdbcRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor(EntityWithClass.class);

		final BasicAttributeMapping duration = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("clazz");
		final JdbcMapping jdbcMapping = duration.getJdbcMapping();
		assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Class.class));
		assertThat( jdbcMapping.getJdbcType(), equalTo( jdbcRegistry.getDescriptor( Types.VARCHAR)));

		scope.inTransaction(
				(session) -> {
					session.persist(new EntityWithClass(1, String.class));
				}
		);

		scope.inTransaction(
				(session) -> session.find(EntityWithClass.class, 1)
		);
	}

	@Entity(name = "EntityWithClass")
	@Table(name = "EntityWithClass")
	public static class EntityWithClass {
		@Id
		private Integer id;

		//tag::basic-Class-example[]
		// mapped as VARCHAR
		private Class<?> clazz;
		//end::basic-Class-example[]

		public EntityWithClass() {
		}

		public EntityWithClass(Integer id, Class<?> clazz) {
			this.id = id;
			this.clazz = clazz;
		}
	}
}
