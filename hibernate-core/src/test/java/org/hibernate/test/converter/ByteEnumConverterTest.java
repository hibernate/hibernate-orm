/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter;

import java.util.Map;
import java.util.HashMap;

import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.query.Query;
import org.hibernate.type.Type;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vincent Sourin
 */
@TestForIssue( jiraKey = "HHH-13334")
public class ByteEnumConverterTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Photo.class };
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Photo photo = new Photo();
			photo.setId( 1 );
			photo.setName( "Dorobantul" );
			photo.setFileType( FileType.TYPE2 );

			entityManager.persist( photo );
		} );
	}

	@Test
	public void testEnumWithByte() throws Exception {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::basic-attribute-converter-query-parameter-converter-dbdata-example[]
			Photo photo = entityManager.createQuery(
				"select p " +
				"from Photo p ", Photo.class )
			.getSingleResult();
			//end::basic-attribute-converter-query-parameter-converter-dbdata-example[]

			assertEquals( FileType.TYPE2, photo.getFileType() );
		} );
	}

	//tag::basic-attribute-converter-query-parameter-object-example[]
	public static enum FileType {
		TYPE1((byte) 1),
		TYPE2((byte) 2);
        
		private byte id;
    
		private FileType(byte value) {
			this.id = value;
		}
    
		public byte getId() {
			return id;
		}
    
		public static FileType getValue(byte value) {
			return map.get(value);
		}
        
		private final static Map<Byte, FileType> map = new HashMap<>();
		static {
			for (FileType s : values())
				map.put(s.id, s);
		}
	}
	//end::basic-attribute-converter-query-parameter-object-example[]

	//tag::basic-attribute-converter-query-parameter-converter-example[]
	public static class FileTypeConverter
			implements AttributeConverter<FileType, Byte> {

		@Override
		public Byte convertToDatabaseColumn(FileType attribute) {
			return attribute.getId();
		}

		@Override
		public FileType convertToEntityAttribute(Byte dbData) {
			return FileType.getValue(dbData);
		}
	}
	//end::basic-attribute-converter-query-parameter-converter-example[]

	//tag::basic-attribute-converter-query-parameter-entity-example[]
	@Entity(name = "Photo")
	public static class Photo {

		@Id
		private Integer id;

		private String name;

		@Convert(converter = FileTypeConverter.class)
		private FileType fileType;

		//Getters and setters are omitted for brevity
	//end::basic-attribute-converter-query-parameter-entity-example[]

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public FileType getFileType() {
			return fileType;
		}

		public void setFileType(FileType fileType) {
			this.fileType = fileType;
		}
	//tag::basic-attribute-converter-query-parameter-entity-example[]
	}
	//end::basic-attribute-converter-query-parameter-entity-example[]
}
