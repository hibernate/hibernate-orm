/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Andrea Boriero
 */
@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({ DirtyCheckEnhancementContext.class })
public class DirtyCheckPrivateUnMappedCollectionTest extends BaseNonConfigCoreFunctionalTestCase {

	boolean skipTest;

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, "100" );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		String byteCodeProvider = Environment.getProperties().getProperty( AvailableSettings.BYTECODE_PROVIDER );
		if ( byteCodeProvider != null && !Environment.BYTECODE_PROVIDER_NAME_BYTEBUDDY.equals( byteCodeProvider ) ) {
			// skip the test if the bytecode provider is Javassist
			skipTest = true;
		}
		else {
			sources.addAnnotatedClass( Measurement.class );
		}
	}

	@Test
	public void testIt() {
		if ( skipTest ) {
			return;
		}
		inTransaction(
				session -> {
					Tag tag = new Tag();
					tag.setName( "tag1" );

					Measurement measurementDescriptor = new Measurement();
					measurementDescriptor.addTag( tag );

					session.save( measurementDescriptor );
				}
		);
	}

	@MappedSuperclass
	public static class AbstractMeasurement {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id;

		private String name;

		@Convert(converter = TagAttributeConverter.class)
		private List<Tag> tags = new ArrayList<>();

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

		public List<Tag> getTags() {
			return tags;
		}

		public void setTags(List<Tag> tags) {
			this.tags = tags;
		}

		public void addTag(Tag tag) {
			this.tags.add( tag );
		}
	}

	@Entity(name = "Measurement")
	public static class Measurement extends AbstractMeasurement {
	}

	public static class Tag {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Converter
	public static class TagAttributeConverter implements AttributeConverter<List<Tag>, String> {

		@Override
		public String convertToDatabaseColumn(List<Tag> attribute) {
			return "empty";
		}

		@Override
		public List<Tag> convertToEntityAttribute(String dbData) {
			return Collections.emptyList();
		}
	}

}
