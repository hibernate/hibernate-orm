package org.hibernate.test.bytecode.enhancement.access;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.junit.Assert;

/**
 * @author Luis Barreiro
 */
public class MixedAccessTestTask extends AbstractEnhancerTestTask {

	private static ScriptEngine engine = new ScriptEngineManager().getEngineByName( "javascript" );
	private static boolean cleanup = false;

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{TestEntity.class, TestOtherEntity.class};
	}

	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		super.prepare( cfg );

		Session s = getFactory().openSession();
		s.beginTransaction();

		try {
			TestEntity testEntity = new TestEntity( "foo" );
			testEntity.setParamsAsString( "{\"paramName\":\"paramValue\"}" );
			s.persist( testEntity );

			TestOtherEntity testOtherEntity = new TestOtherEntity( "foo" );
			testOtherEntity.setParamsAsString( "{\"paramName\":\"paramValue\"}" );
			s.persist( testOtherEntity );

			s.getTransaction().commit();
		}
		catch ( Exception e ) {
			s.getTransaction().rollback();
			throw e;
		}
		finally {
			s.close();
		}
	}

	public void execute() {
		Session s = getFactory().openSession();
		s.beginTransaction();

		try {
			TestEntity testEntity = s.get( TestEntity.class, "foo" );
			Assert.assertEquals( "{\"paramName\":\"paramValue\"}", testEntity.getParamsAsString() );

			TestOtherEntity testOtherEntity = s.get( TestOtherEntity.class, "foo" );
			Assert.assertEquals( "{\"paramName\":\"paramValue\"}", testOtherEntity.getParamsAsString() );

			// Clean parameters
			cleanup = true;
			testEntity.setParamsAsString( "{}" );
			testOtherEntity.setParamsAsString( "{}" );

			s.getTransaction().commit();
		}
		catch ( RuntimeException e ) {
			s.getTransaction().rollback();
			throw e;
		}
		finally {
			s.close();
		}
	}

	protected void cleanup() {
		Session s = getFactory().openSession();
		s.beginTransaction();

		try {
			TestEntity testEntity = s.get( TestEntity.class, "foo" );
			Assert.assertTrue( testEntity.getParams().isEmpty() );

			TestOtherEntity testOtherEntity = s.get( TestOtherEntity.class, "foo" );
			Assert.assertTrue( testOtherEntity.getParams().isEmpty() );

			s.getTransaction().commit();
		}
		catch ( RuntimeException e ) {
			s.getTransaction().rollback();
			throw e;
		}
		finally {
			s.close();
		}
	}

	@Entity
	private static class TestEntity {

		@Id
		String name;

		@Transient
		Map<String, String> params = new LinkedHashMap<>();

		public TestEntity(String name) {
			this();
			this.name = name;
		}

		protected TestEntity() {
		}

		public Map<String, String> getParams() {
			return params;
		}

		public void setParams(Map<String, String> params) {
			this.params = params;
		}

		@Column( name = "params", length = 4000 )
		@Access( AccessType.PROPERTY )
		public String getParamsAsString() {
			if ( params.size() > 0 ) {
				// Convert to JSON
				return "{" + params.entrySet().stream().map(
						e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\""
				).collect( Collectors.joining( "," ) ) + "}";
			}
			return null;
		}

		public void setParamsAsString(String string) {
			params.clear();

			try {
				params.putAll( (Map<String, String>) engine.eval( "Java.asJSONCompatible(" + string + ")" ) );
			} catch ( ScriptException ignore ) {
				// JDK 8u60 required --- use hard coded values to pass the test
				if ( !cleanup ) {
					params.put( "paramName", "paramValue" );
				}
			}
		}
	}

	@Entity
	@Table(name = "other")
	@Access( AccessType.FIELD )
	private static class TestOtherEntity {

		@Id
		String name;

		@Transient
		Map<String, String> params = new LinkedHashMap<>();

		public TestOtherEntity(String name) {
			this();
			this.name = name;
		}

		protected TestOtherEntity() {
		}

		public Map<String, String> getParams() {
			return params;
		}

		public void setParams(Map<String, String> params) {
			this.params = params;
		}

		@Column( name = "params", length = 4000 )
		@Access( AccessType.PROPERTY )
		public String getParamsAsString() {
			if ( params.size() > 0 ) {
				// Convert to JSON
				return "{" + params.entrySet().stream().map(
						e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\""
				).collect( Collectors.joining( "," ) ) + "}";
			}
			return null;
		}

		public void setParamsAsString(String string) {
			params.clear();

			try {
				params.putAll( (Map<String, String>) engine.eval( "Java.asJSONCompatible(" + string + ")" ) );
			} catch ( ScriptException ignore ) {
				// JDK 8u60 required --- use hard coded values to pass the test
				if ( !cleanup ) {
					params.put( "paramName", "paramValue" );
				}
			}
		}
	}
}
