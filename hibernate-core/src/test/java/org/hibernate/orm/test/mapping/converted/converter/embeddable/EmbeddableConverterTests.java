/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.mapping.converted.converter.embeddable;

import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = TheEntity.class )
public class EmbeddableConverterTests {
	@Test
	@NotImplementedYet( strict = false )
	public void verifyModel(DomainModelScope scope) {
		scope.withHierarchy( TheEntity.class, (descriptor) -> {
			final Property property = descriptor.getProperty( "name" );
			final Component value = (Component) property.getValue();
			final ConverterDescriptor converter = value.getJpaAttributeConverterDescriptor();
			// converters for embeddable values are not yet understood
			assertThat( converter ).isNotNull();
			assertThat( converter.getAttributeConverterClass() ).isEqualTo( NameConverter.class );
		} );
	}
}
