/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultmapping;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.internal.NamedResultSetMappingMementoImpl;
import org.hibernate.query.internal.ResultMementoInstantiationStandard;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.named.ResultMemento;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class XmlOverrideTests {
	@Test
	@DomainModel(
			annotatedClasses = SimpleEntity.class,
			xmlMappings = "mappings/query/simple-resultset-mapping-override.xml"
	)
	@SessionFactory
	void testResultSetMappingOverride(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final NamedObjectRepository namedObjectRepository = sessionFactory.getQueryEngine().getNamedObjectRepository();
		final NamedResultSetMappingMemento dtoMappingMemento = namedObjectRepository.getResultSetMappingMemento( "dto-mapping" );
		final NamedResultSetMappingMementoImpl mementoImpl = (NamedResultSetMappingMementoImpl) dtoMappingMemento;
		final List<ResultMemento> resultMementos = mementoImpl.getResultMementos();
		assertThat( resultMementos ).hasSize( 1 );

		final ResultMemento resultMemento = resultMementos.get( 0 );
		assertThat( resultMemento ).isInstanceOf( ResultMementoInstantiationStandard.class );
		final ResultMementoInstantiationStandard ctorMemento = (ResultMementoInstantiationStandard) resultMemento;
		final Class<?> resultClass = ctorMemento.getInstantiatedJavaType().getJavaTypeClass();
		assertThat( resultClass ).isEqualTo( Dto2.class );
	}

	@Entity(name="SimpleEntity")
	@Table(name="SimpleEntity")
	@SqlResultSetMappings({
			@SqlResultSetMapping( name = "dto-mapping", classes = {
					@ConstructorResult( targetClass = Dto1.class, columns = {
							@ColumnResult( name = "id", type = Integer.class ),
							@ColumnResult( name = "name", type = String.class )
					})
			}),
			@SqlResultSetMapping( name = "irrelevant", columns = @ColumnResult( name = "name", type = String.class ) )
	})
	public static class SimpleEntity {
		@Id
		private Integer id;
		private String name;
	}

	public record Dto1(Integer id, String name) {
	}

	public record Dto2(Integer id, String name) {
	}
}
