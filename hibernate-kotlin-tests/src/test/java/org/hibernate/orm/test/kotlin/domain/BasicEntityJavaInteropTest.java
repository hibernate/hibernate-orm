package org.hibernate.orm.test.kotlin.domain;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kotlin-from-Java interop: a Java test consuming the Kotlin entity
 */
@DomainModel(annotatedClasses = BasicEntity.class)
@SessionFactory
public class BasicEntityJavaInteropTest {

	@Test
	public void testKotlinEntityFromJava(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new BasicEntity( 1, "data_1" ) ) );

		scope.inTransaction( session ->
				assertThat( session.find( BasicEntity.class, 1 ) )
						.isEqualTo( new BasicEntity( 1, "data_1" ) ) );
	}
}
