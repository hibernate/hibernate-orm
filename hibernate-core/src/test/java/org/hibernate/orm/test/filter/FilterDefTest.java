package org.hibernate.orm.test.filter;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.ParamDef;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@JiraKey( "HHH-16581" )
public class FilterDefTest {

	@Test
	public void testLegalFilterDefRepetition() {
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build();
		try {
			new MetadataSources( serviceRegistry )
					.addAnnotatedClass( EntityA.class )
					.addAnnotatedClass( EntityB.class )
					.addAnnotatedClass( EntityA1.class )
					.addAnnotatedClass( EntityB1.class )
					.addAnnotatedClass( EntityA2.class )
					.addAnnotatedClass( EntityB2.class )
					.buildMetadata();

		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testIllegalFilterDefRepetition() {
		assertThrows( AnnotationException.class, () -> {
			StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build();
			try {
				new MetadataSources( serviceRegistry )
						.addAnnotatedClass( EntityA.class )
						.addAnnotatedClass( EntityC.class )
						.buildMetadata();
				fail( "AnnotationException expected" );
			}
			finally {
				StandardServiceRegistryBuilder.destroy( serviceRegistry );
			}
		} );

		assertThrows( AnnotationException.class, () -> {
			StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build();
			try {
				new MetadataSources( serviceRegistry )
						.addAnnotatedClass( EntityC.class )
						.addAnnotatedClass( EntityD.class )
						.buildMetadata();
				fail( "AnnotationException expected" );
			}
			finally {
				StandardServiceRegistryBuilder.destroy( serviceRegistry );
			}
		} );

		assertThrows( AnnotationException.class, () -> {
			StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build();
			try {
				new MetadataSources( serviceRegistry )
						.addAnnotatedClass( EntityD.class )
						.addAnnotatedClass( EntityE.class )
						.buildMetadata();
				fail( "AnnotationException expected" );
			}
			finally {
				StandardServiceRegistryBuilder.destroy( serviceRegistry );
			}
		} );
	}

	@FilterDefs(value = @FilterDef(name = "filterA"))
	@Entity(name = "EntityA")
	public static class EntityA {
		@Id
		private long id;

		private String name;
	}

	@FilterDefs(value = @FilterDef(name = "filterA"))
	@Entity(name = "EntityB")
	public class EntityB {
		@Id
		private long id;

		private String name;
	}

	@FilterDefs(value = @FilterDef(name = "filterB", defaultCondition = "name = :name"))
	@Entity(name = "EntityA1")
	public static class EntityA1 {
		@Id
		private long id;

		private String name;
	}

	@FilterDefs(value = @FilterDef(name = "filterB", defaultCondition = "name = :name"))
	@Entity(name = "EntityB1")
	public class EntityB1 {
		@Id
		private long id;

		private String name;
	}

	@FilterDefs(value = @FilterDef(name = "filterC", defaultCondition = "name = :name", parameters = @ParamDef(name = "name", type = String.class)))
	@Entity(name = "EntityA2")
	public static class EntityA2 {
		@Id
		private long id;

		private String name;
	}

	@FilterDefs(value = @FilterDef(name = "filterC", defaultCondition = "name = :name", parameters = @ParamDef(name = "name", type = String.class)))
	@Entity(name = "EntityB2")
	public class EntityB2 {
		@Id
		private long id;

		private String name;
	}

	@FilterDefs(value = @FilterDef(name = "filterA", defaultCondition = "name = :name"))
	@Entity(name = "EntityC")
	public class EntityC {
		@Id
		private long id;

		private String name;
	}

	@FilterDefs(value = @FilterDef(name = "filterA", defaultCondition = "name = :name", parameters = @ParamDef(name = "name", type = String.class)))
	@Entity(name = "EntityD")
	public class EntityD {
		@Id
		private long id;

		private String name;
	}

	@FilterDefs(value = @FilterDef(name = "filterA", defaultCondition = "name = :name", parameters = @ParamDef(name = "name", type = Integer.class)))
	@Entity(name = "EntityE")
	public class EntityE {
		@Id
		private long id;

		private String name;
	}
}
