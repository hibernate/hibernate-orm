/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.integration.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.Session;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.query.NativeQuery;
import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.GeomCodec;
import org.hibernate.spatial.testing.HQLTemplate;
import org.hibernate.spatial.testing.NativeSQLTemplate;
import org.hibernate.type.Type;

import org.hibernate.testing.orm.junit.SessionFactoryScope;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class FunctionTestTemplate {

	final private CommonSpatialFunction spatialFunction;
	final private HQLTemplate hqlTemplate;
	final private NativeSQLTemplate sqlTemplate;
	final private RowObjectMapper rowObjectMapper;
	final private Model model;
	final private List<Param> parameters;
	final private GeomCodec codec;

	FunctionTestTemplate(
			CommonSpatialFunction function,
			HQLTemplate hqlTemplate,
			NativeSQLTemplate sqlTemplate,
			RowObjectMapper rowObjectMapper,
			Model model,
			List<Param> params,
			GeomCodec codec) {
		this.spatialFunction = function;
		this.hqlTemplate = hqlTemplate;
		this.sqlTemplate = sqlTemplate;
		this.rowObjectMapper = rowObjectMapper;
		this.model = model;
		this.parameters = params;
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

	private NativeQuery<Object> createNativeQuery(Session session, String table) {
		return session.createNativeQuery( sqlTemplate.mkNativeSQLString( table ) );
	}

	private List<Object> map(List<Object> list) {
		Stream<Object> stream = list
				.stream().map( this::mapRow );
		return stream.collect( Collectors.toList() );
	}


	private Object mapRow(Object object) {
		Data data = rowObjectMapper.apply( object );
		if ( this.spatialFunction.returnsGeometry() ) {
			data.datum = this.model.from.apply( codec.toGeometry( data.datum ) );
			return data;
		}
		return data;
	}

	public List executeHQL(SessionFactoryScope scope, String functionName) {
		final AtomicReference<List> results = new AtomicReference<>();
		final String entity = model.entityClass.getCanonicalName();
		scope.inSession(
				session -> results.set( session.createQuery(
						hqlTemplate.mkHQLString( functionName, entity ) ).getResultList() ) );
		return (List) results.get().stream().map( rowObjectMapper::apply ).collect( Collectors.toList() );
	}

	static class Param {
		final Object value;
		final Type type;

		public Param(Object value, Type type) {
			this.value = value;
			this.type = type;
		}
	}

	static class Builder {
		CommonSpatialFunction key;
		HQLTemplate hql = new HQLTemplate( "select id, %s(geom) from %s" );
		NativeSQLTemplate sql;
		RowObjectMapper mapper;
		List<Param> params = new ArrayList<>();

		FunctionTestTemplate build(Model model, GeomCodec codec) {
			if ( this.mapper == null ) {
				this.mapper = new RowObjectMapper() {
				};
			}
			return new FunctionTestTemplate( key, hql, sql, mapper, model, params, codec );
		}

		Builder key(CommonSpatialFunction key) {
			this.key = key;
			return this;
		}

		Builder hql(String hqlString) {
			this.hql = new HQLTemplate( hqlString );
			return this;
		}

		Builder sql(String sqlString) {
			this.sql = new NativeSQLTemplate( sqlString );
			return this;
		}

		Builder parameter(Object value, Type type) {
			this.params.add( new Param( value, type ) );
			return this;
		}
	}
}
