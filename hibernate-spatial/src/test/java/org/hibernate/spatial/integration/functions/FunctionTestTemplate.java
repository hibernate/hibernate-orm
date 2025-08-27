/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.integration.functions;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.Session;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.GeomCodec;
import org.hibernate.spatial.integration.Model;
import org.hibernate.spatial.testing.HQLTemplate;
import org.hibernate.spatial.testing.NativeSQLTemplate;
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.GeometryEquality;
import org.geolatte.geom.codec.Wkt;

/**
 * Represents the template from which a Dynamic test can be generated.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class FunctionTestTemplate {

	final private CommonSpatialFunction spatialFunction;
	final private HQLTemplate hqlTemplate;
	final private NativeSQLTemplate sqlTemplate;
	final private RowObjectMapper rowObjectMapper;
	final private Model model;
	final private Geometry<?> testGeometry;
	final private GeomCodec codec;

	FunctionTestTemplate(
			CommonSpatialFunction function,
			HQLTemplate hqlTemplate,
			NativeSQLTemplate sqlTemplate,
			RowObjectMapper rowObjectMapper,
			Model model,
			Geometry<?> testGeometry,
			GeomCodec codec) {
		this.spatialFunction = function;
		this.hqlTemplate = hqlTemplate;
		this.sqlTemplate = sqlTemplate;
		this.rowObjectMapper = rowObjectMapper;
		this.model = model;
		this.testGeometry = testGeometry;
		this.codec = codec;
	}


	public String getFunctionName() {
		return this.spatialFunction.getKey().getName();
	}

	public String getAltFunctionName() {
		return this.spatialFunction.getKey().getAltName().orElse( null );
	}

	public Model getModel() {
		return model;
	}

	public List<Object> executeNativeQuery(SessionFactoryScope scope) {
		PersistentClass binding = scope.getMetadataImplementor()
				.getEntityBinding( model.entityClass.getCanonicalName() );
		String table = binding.getTable().getName();
		final AtomicReference<List<Object>> results = new AtomicReference<>();
		scope.inSession( session -> results.set( createNativeQuery( session, table ).getResultList() ) );
		return map( results.get() );
	}

	private NativeQuery createNativeQuery(Session session, String table) {
		NativeQuery query = session.createNativeQuery( sqlTemplate.mkNativeSQLString( table ) );
		if ( spatialFunction.getReturnType() != null ) {
			query.addScalar( "id", StandardBasicTypes.INTEGER );
			query.addScalar( "result", spatialFunction.getReturnType() );
		}
		if ( testGeometry != null ) {
			query.setParameter( "filter", Wkt.toWkt( testGeometry, Wkt.Dialect.SFA_1_1_0 ) );
		}

		return query;
	}

	public List executeHQL(SessionFactoryScope scope, String functionName) {
		final String entity = model.entityClass.getCanonicalName();
		final AtomicReference<List<Object>> results = new AtomicReference<>();
		scope.inSession(
				session -> {
					Query query = session.createQuery( hqlTemplate.mkHQLString( functionName, entity ) );
					if ( testGeometry != null ) {
						query.setParameter(
								"filter",
								getModel().from.apply( testGeometry )
						);
					}
					results.set( query.getResultList() );
				} );
		return results.get().stream().map( rowObjectMapper::apply ).collect( Collectors.toList() );
	}

	//only for JtsGeometry because extra mapping of native Geometry object (where needed)
	private List<Object> map(List<Object> list) {
		Stream<Object> stream = list
				.stream().map( this::mapRow );
		return stream.collect( Collectors.toList() );
	}


	private Object mapRow(Object object) {
		Data data = rowObjectMapper.apply( object );
		if ( this.spatialFunction.returnsGeometry() ) {
			data.datum = data.datum != null ? this.model.from.apply( codec.toGeometry( data.datum ) ) : null;
			return data;
		}
		return data;
	}

	/**
	 * A Builder for a {@code FunctionTestTemplate}
	 */
	static class Builder {
		CommonSpatialFunction function;
		HQLTemplate hql;
		NativeSQLTemplate sql;
		RowObjectMapper mapper;
		Geometry<?> testGeometry;

		GeometryEquality geomEq;

		public Builder(CommonSpatialFunction function) {
			this.function = function;
		}

		//on building the instance, inject the relevant Geometry model and context-specific native->Geometry codec
		FunctionTestTemplate build(Model model, GeomCodec codec) {
			if ( hql == null ) {
				if ( testGeometry != null ) {
					hql = new HQLTemplate( "select id, %s(geom, :filter) from %s" );
				}
				else if ( function == CommonSpatialFunction.ST_BUFFER ) {
					hql = new HQLTemplate( "select id, %s(geom, 2) from %s" );
				}
				else {
					hql = new HQLTemplate( "select id, %s(geom) from %s" );
				}
			}
			if ( this.mapper == null ) {
				this.mapper = new RowObjectMapper( geomEq ) {
				};
			}
			return new FunctionTestTemplate( function, hql, sql, mapper, model, testGeometry, codec );
		}

		Builder hql(String hqlString) {
			if ( hqlString != null ) {
				this.hql = new HQLTemplate( hqlString );
			}
			return this;
		}

		Builder sql(String sqlString) {
			this.sql = new NativeSQLTemplate( sqlString );
			return this;
		}

		Builder geometry(Geometry<?> value) {
			if ( value != null ) {
				this.testGeometry = value;
			}
			return this;
		}

		public Builder equalityTest(GeometryEquality geomEq) {
			this.geomEq = geomEq;
			return this;
		}
	}
}
