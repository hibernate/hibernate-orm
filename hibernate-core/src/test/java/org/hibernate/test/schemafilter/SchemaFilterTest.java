package org.hibernate.test.schemafilter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.tool.schema.internal.DefaultSchemaFilter;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.junit.Assert;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-9876")
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SchemaFilterTest extends BaseUnitTestCase {

	private final ServiceRegistry serviceRegistry;
	private final Metadata metadata;
	
	public SchemaFilterTest() {
		Map settings = new HashMap();
		settings.putAll( Environment.getProperties() );
		settings.put( AvailableSettings.DIALECT, SQLServerDialect.class.getName() );

		this.serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( settings );

		MetadataSources ms = new MetadataSources( serviceRegistry );
		ms.addAnnotatedClass( SchemaNoneEntity0.class );
		ms.addAnnotatedClass( Schema1Entity1.class );
		ms.addAnnotatedClass( Schema1Entity2.class );
		ms.addAnnotatedClass( Schema2Entity3.class );
		ms.addAnnotatedClass( Schema2Entity4.class );
		this.metadata = ms.buildMetadata();
	}
	
	@Test
	public void createSchema_unfiltered() {
		RecordingTarget target = doCreation( new DefaultSchemaFilter() );
		
		Assert.assertThat( target.getActions( "schema.create" ), containsExactly( "the_schema_1", "the_schema_2" ));
		Assert.assertThat( target.getActions( "table.create" ), containsExactly( 
				"the_entity_0", 
				"the_schema_1.the_entity_1", 
				"the_schema_1.the_entity_2", 
				"the_schema_2.the_entity_3", 
				"the_schema_2.the_entity_4" 
			));
	}
	
	@Test
	public void createSchema_filtered() {
		RecordingTarget target = doCreation( new TestSchemaFilter() );
		
		Assert.assertThat( target.getActions( "schema.create" ), containsExactly( "the_schema_1" ));
		Assert.assertThat( target.getActions( "table.create" ), containsExactly( "the_entity_0", "the_schema_1.the_entity_1" ));
	}
	
	@Test
	public void dropSchema_unfiltered() {
		RecordingTarget target = doDrop( new DefaultSchemaFilter() );
		
		Assert.assertThat( target.getActions( "schema.drop" ), containsExactly( "the_schema_1", "the_schema_2" ));
		Assert.assertThat( target.getActions( "table.drop" ), containsExactly(
				"the_entity_0", 
				"the_schema_1.the_entity_1", 
				"the_schema_1.the_entity_2", 
				"the_schema_2.the_entity_3", 
				"the_schema_2.the_entity_4" 
			));
	}

	@Test
	public void dropSchema_filtered() {
		RecordingTarget target = doDrop( new TestSchemaFilter() );
		
		Assert.assertThat( target.getActions( "schema.drop" ), containsExactly( "the_schema_1" ));
		Assert.assertThat( target.getActions( "table.drop" ), containsExactly( "the_entity_0", "the_schema_1.the_entity_1" ));
	}

	private RecordingTarget doCreation( SchemaFilter filter ) {
		RecordingTarget target = new RecordingTarget();
		SchemaCreator creator = new SchemaCreatorImpl( filter );
		creator.doCreation( metadata, true, target );
		return target;
	}
	
	private RecordingTarget doDrop( SchemaFilter filter ) {
		RecordingTarget target = new RecordingTarget();
		SchemaDropper dropper = new SchemaDropperImpl( filter );
		dropper.doDrop( metadata, true, target );
		return target;
	}

	private BaseMatcher<Set<String>> containsExactly( Object... expected ) {
		return containsExactly( new HashSet<>( Arrays.asList( expected ) ) );
	}
	
	private BaseMatcher<Set<String>> containsExactly( final Set expected ) {
		return new BaseMatcher<Set<String>>() {
			@Override
			public boolean matches( Object item ) {
				Set set = (Set) item;
				return set.size() == expected.size() 
					&& set.containsAll( expected );
			}

			@Override
			public void describeTo( Description description ) {
				description.appendText( "Is set containing exactly " + expected );
			}
		};
	}

	private static class TestSchemaFilter implements SchemaFilter {
		
		@Override
		public boolean includeNamespace( Namespace namespace ) {
			// exclude schema "the_schema_2"
			Identifier identifier = namespace.getName().getSchema();
			if ( identifier != null ) {
				return !"the_schema_2".equals( identifier.getText() );
			}
			return true;
		}

		@Override
		public boolean includeTable( Table table ) {
			// exclude table "the_entity_2"
			return !"the_entity_2".equals( table.getName() );
		}

		@Override
		public boolean includeSequence( Sequence sequence ) {
			return true;
		}
	}
}
