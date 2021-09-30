/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic;

import java.sql.Types;
import java.util.BitSet;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isOneOf;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = BitSetConverterTests.Product.class )
@SessionFactory
public class BitSetConverterTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final MappingMetamodel domainModel = sessionFactory.getDomainModel();

		final EntityPersister entityDescriptor = domainModel.findEntityDescriptor( Product.class );
		final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( "bitSet" );

		assertThat( attributeMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo( BitSet.class ) );

		assertThat( attributeMapping.getValueConverter(), instanceOf( JpaAttributeConverter.class ) );
		final JpaAttributeConverter converter = (JpaAttributeConverter) attributeMapping.getValueConverter();
		assertThat( converter.getConverterBean().getBeanClass(), equalTo( BitSetConverter.class ) );

		assertThat(
				attributeMapping.getJdbcMapping().getJdbcTypeDescriptor().getJdbcTypeCode(),
				isOneOf( Types.VARCHAR, Types.NVARCHAR )
		);

		assertThat( attributeMapping.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass(), equalTo( String.class ) );
	}

	@Table(name = "products")
	//tag::basic-bitset-example-convert[]
	@Entity(name = "Product")
	public static class Product {
		@Id
		private Integer id;

		@Convert( converter = BitSetConverter.class )
		private BitSet bitSet;

		//Getters and setters are omitted for brevity
		//end::basic-bitset-example-convert[]

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public BitSet getBitSet() {
			return bitSet;
		}

		public void setBitSet(BitSet bitSet) {
			this.bitSet = bitSet;
		}
		//tag::basic-bitset-example-convert[]
	}
	//end::basic-bitset-example-convert[]


	//tag::basic-bitset-example-converter[]
	@Converter( autoApply = true )
	public static class BitSetConverter implements AttributeConverter<BitSet,String> {
		@Override
		public String convertToDatabaseColumn(BitSet attribute) {
			return BitSetHelper.bitSetToString( attribute );
		}

		@Override
		public BitSet convertToEntityAttribute(String dbData) {
			return BitSetHelper.stringToBitSet( dbData );
		}
	}
	//end::basic-bitset-example-converter[]
}
