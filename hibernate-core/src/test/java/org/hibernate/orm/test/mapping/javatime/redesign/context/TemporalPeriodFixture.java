package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;

import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Temporal;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.mapping.BasicValue;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/// Shared live period fixture. Expected instants and raw column decoding are independent
/// of Hibernate's period extractor and producer SQL. All connections are fixture-owned.
///
/// @author Steve Ebersole
final class TemporalPeriodFixture implements AutoCloseable {
	static final Instant[] SEASONAL = {
			Instant.parse("2024-01-15T12:00:00.123456Z"),
			Instant.parse("2024-07-15T12:00:00.123456Z"),
			Instant.parse("2024-07-16T12:00:00.123456Z")
	};
	static final Instant[] FOLD = {
			Instant.parse("2024-11-03T07:30:00.123456Z"),
			Instant.parse("2024-11-03T08:30:00.123456Z"),
			Instant.parse("2024-11-03T09:30:00.123456Z")
	};

	final ContextScope scope;
	final Class<? extends Row> type;
	final Fixed supplier;
	final Connection writer, monitor, reader;
	final String entityTable, collectionTable;
	final int precision;
	final long tick;
	final boolean server, zoned;

	static Map<String,Object> settings(boolean preservation, String strategy, boolean server, Fixed supplier) {
		var settings = new HashMap<String,Object>();
		settings.put("hibernate.type.temporal_preservation",preservation);
		settings.put("hibernate.temporal.table_strategy",strategy);
		settings.put("hibernate.test.temporal.reuse_connection",true);
		settings.put("hibernate.cache.use_second_level_cache",false);
		if(server) settings.put("hibernate.temporal.use_server_transaction_timestamps",true);
		else settings.put("hibernate.temporal.changeset_id_supplier",supplier);
		return settings;
	}

	TemporalPeriodFixture(boolean metadata, String strategy, boolean server, Class<? extends Row> type) throws Exception {
		this(metadata,strategy,server,type,true,true,Map.of());
	}

	TemporalPeriodFixture(boolean metadata, String strategy, boolean server, Class<? extends Row> type,
			boolean preservation, boolean provision, Map<String,Object> extra) throws Exception {
		this.type=type; this.server=server; supplier=new Fixed();
		var settings=settings(preservation,strategy,server,supplier); settings.putAll(extra);
		scope=provision ? ContextScope.open(metadata,false,settings,List.of(),type)
				:ContextScope.boot(metadata,false,settings,type);
		scope.provider.blocked=false;
		writer=scope.provider.getConnection(); monitor=scope.provider.getConnection(); reader=scope.provider.getConnection();
		assertThat(writer).isNotSameAs(monitor).isNotSameAs(reader); assertThat(reader).isNotSameAs(monitor);
		var identities = List.of( identity( writer ), identity( monitor ), identity( reader ) );
		assertThat( new HashSet<>( identities ) ).hasSize( 3 );
		System.out.println( "PERIOD_CONNECTIONS writer/monitor/reader=" + identities
				+ " JVM=" + ZoneId.systemDefault()
				+ " session=" + System.getProperty( "hibernate.test.temporal.session_zone", "provider default" )
				+ " JDBC=" + System.getProperty( "hibernate.jdbc.time_zone", "unset" ) );
		for(var connection:List.of(writer,monitor,reader)) assertSessionZone(connection);
		var model=scope.metadata.getEntityBinding(type.getName());
		entityTable=model.getAuxiliaryTable().getName();
		collectionTable=scope.metadata.getCollectionBinding(type.getName()+".tags").getAuxiliaryTable().getName();
		var start=model.getAuxiliaryColumn("rowStart");
		precision=start.getTemporalPrecision()==null?scope.factory.getJdbcServices().getDialect().getTemporalTableSupport().getTemporalColumnPrecision():start.getTemporalPrecision();
		tick=(long)Math.pow(10,9-precision);
		var value=(BasicValue)start.getValue();
		zoned=scope.factory.getJdbcServices().getDialect() instanceof PostgreSQLDialect;
		System.out.println("PERIOD_PROFILE type="+type.getName()+" metadata="+metadata+" preserving="+preservation+" strategy="+strategy
				+" server="+server+" inline="+scope.factory.getChangesetCoordinator().useServerTimestamp(scope.factory.getJdbcServices().getDialect())
				+" jdbc="+value.resolve().getJdbcType().getClass().getName()+" ddl="+start.getSqlType(scope.metadata)+" precision="+precision
				+" database="+writer.getMetaData().getDatabaseProductVersion()+" driver="+writer.getMetaData().getDriverVersion());
		for(var table:List.of(entityTable,collectionTable)) {
			try(var st=reader.createStatement();var rs=st.executeQuery("select valid_from,valid_to from "+table+" where 1=0")) {
				assertThat(rs.getMetaData().getScale(1)).isEqualTo(table.equals(entityTable)?precision:6);
				assertThat(rs.getMetaData().getScale(2)).isEqualTo(table.equals(entityTable)?precision:6);
			}
		}
		reader.commit();
	}

	String identity(Connection c) throws SQLException {
		String sql=zonedDialect()?"select pg_backend_pid()":oracle()?"select sys_context('USERENV','SID') from dual":"select session_id()";
		try(var st=c.createStatement();var rs=st.executeQuery(sql)){rs.next();String id=rs.getString(1);c.commit();return id;}
	}
	boolean zonedDialect(){return scope.factory.getJdbcServices().getDialect() instanceof PostgreSQLDialect;}
	boolean oracle(){return scope.factory.getJdbcServices().getDialect() instanceof OracleDialect;}
	void assertSessionZone(Connection c) throws SQLException {
		String expected=System.getProperty("hibernate.test.temporal.session_zone");
		if(expected==null)return;
		String sql=zonedDialect()?"show time zone":oracle()?"select sessiontimezone from dual":"select setting_value from information_schema.settings where setting_name='TIME ZONE'";
		try(var st=c.createStatement();var rs=st.executeQuery(sql)){assertThat(rs.next()).isTrue();assertThat(ZoneId.of(rs.getString(1)).normalized()).isEqualTo(ZoneId.of(expected).normalized());}
		c.commit();
	}
	Instant clock() throws SQLException {
		try(var st=monitor.createStatement();var rs=st.executeQuery("select current_timestamp"+(oracle()?" from dual":""))){
			rs.next();Instant result=rs.getObject(1,OffsetDateTime.class).toInstant();monitor.commit();return result;
		}
	}
	void advancePast(Instant previous) throws Exception {
		long deadline=System.nanoTime()+5_000_000_000L;
		while(!clock().isAfter(previous.plusNanos(tick))) {
			assertThat(System.nanoTime()).as("database clock advanced by a representable tick").isLessThan(deadline);
			Thread.sleep(1);
		}
	}
	void tx(Consumer<Session> action) {
		try(var session=scope.factory.withOptions().connection(writer).open()) {
			var tx=session.beginTransaction();
			try{action.accept(session);tx.commit();}catch(RuntimeException|Error failure){tx.rollback();throw failure;}
		}
	}
	Row row(int id) {
		try{Row row=type.getDeclaredConstructor().newInstance();row.id=id;row.tags.add("one");return row;}
		catch(ReflectiveOperationException e){throw new AssertionError(e);}
	}
	void insert(int id){tx(s->s.persist(row(id)));}
	void update(int id){tx(s->{Row row=s.find(type,id);row.payload="v2";row.tags.add("two");});}
	void delete(int id){tx(s->s.remove(s.find(type,id)));}

	record Interval(Instant start,Instant end,String value,int position) {}
	List<Interval> intervals(boolean collection,int id) throws SQLException {
		String table=collection?collectionTable:entityTable;
		String key=collection?"owner_id":"id";
		String projection=collection?"tag_value,item_order":"payload,0";
		var result=new ArrayList<Interval>();
		try(var st=reader.prepareStatement("select valid_from,valid_to,"+projection+" from "+table+" where "+key+"=? order by valid_from")) {
			st.setInt(1,id);try(var rs=st.executeQuery()) {
				while(rs.next())result.add(new Interval(instant(rs,1),instant(rs,2),rs.getString(3),rs.getInt(4)));
			}
		}
		reader.commit();return result;
	}
	Instant instant(ResultSet rs,int index) throws SQLException {
		if(zoned){var value=rs.getObject(index,OffsetDateTime.class);return value==null?null:value.toInstant();}
		var value=rs.getObject(index,LocalDateTime.class);return value==null?null:value.toInstant(ZoneOffset.UTC);
	}
	void at(int id,Instant point,String payload,List<String> tags) {
		try(var session=scope.factory.withOptions().connection(reader).asOf(point).open()) {
			var tx=session.beginTransaction();
			try {Row row=session.find(type,id);
				if(payload==null)assertThat(row).isNull();
				else {assertThat(row.payload).isEqualTo(payload);assertThat(row.tags).containsExactlyElementsOf(tags);}
				tx.commit();
			}catch(RuntimeException|Error failure){tx.rollback();throw failure;}
		}
	}
	Instant collectionBoundaryAfter(int id, Instant expected, Runnable action) throws Exception {
		var before = intervals( true, id );
		if ( server && !before.isEmpty() ) {
			advancePast( before.stream().flatMap( row -> java.util.stream.Stream.of( row.start(), row.end() ) )
					.filter( Objects::nonNull ).max( Instant::compareTo ).orElseThrow() );
		}
		supplier.value = expected;
		Instant low = clock(); action.run(); Instant high = clock();
		assertThat( high ).isAfterOrEqualTo( low );
		Instant boundary = intervals( true, id ).stream()
				.flatMap( row -> java.util.stream.Stream.of( row.start(), row.end() ) )
				.filter( Objects::nonNull ).max( Instant::compareTo ).orElseThrow();
		if ( server ) assertThat( boundary ).isBetween( low.minusNanos( tick ), high.plusNanos( tick ) );
		else assertThat( boundary ).isEqualTo( expected );
		System.out.println( "PERIOD_COLLECTION_BOUNDARY low=" + low + " value=" + boundary + " high=" + high );
		return boundary;
	}

	void assertHistory(int id,Instant a,Instant b,Instant c) throws SQLException {
		assertThat(intervals(false,id)).containsExactly(new Interval(a,b,"v1",0),new Interval(b,c,"v2",0));
		var entries=intervals(true,id);
		for(var interval:entries){
			assertThat(interval.start()).isIn(a,b);assertThat(interval.end()).isIn(b,c);
			assertThat(interval.start()).isBefore(interval.end());
		}
		for(var point:List.of(a,b.minusNanos(tick),b,c.minusNanos(tick))) {
			var active=entries.stream().filter(e->!point.isBefore(e.start())&&point.isBefore(e.end())).sorted(Comparator.comparingInt(Interval::position)).map(Interval::value).toList();
			assertThat(active).containsExactlyElementsOf(point.isBefore(b)?List.of("one"):List.of("one","two"));
		}
		at(id,a.minusNanos(tick),null,List.of());at(id,a,"v1",List.of("one"));at(id,b.minusNanos(tick),"v1",List.of("one"));
		at(id,b,"v2",List.of("one","two"));at(id,c.minusNanos(tick),"v2",List.of("one","two"));at(id,c,null,List.of());
	}
	Instant[] lifecycle(int id,Instant[] supplied) throws Exception {
		Instant[] points=new Instant[3];
		for(int phase=0;phase<3;phase++) {
			if(server&&phase>0)advancePast(points[phase-1]);
			supplier.value=supplied[phase];
			Instant low=clock();int calls=supplier.calls;
			if(phase==0)insert(id);else if(phase==1)update(id);else delete(id);
			Instant high=clock();assertThat(high).isAfterOrEqualTo(low);
			var rows=intervals(false,id);
			points[phase]=phase==2?rows.get(rows.size()-1).end():rows.get(rows.size()-1).start();
			if(server)assertThat(points[phase]).isBetween(low.minusNanos(tick),high.plusNanos(tick));
			else {assertThat(points[phase]).isEqualTo(supplied[phase]);assertThat(supplier.calls-calls).isEqualTo(1);}
			System.out.println("PERIOD_BOUNDARY id="+id+" phase="+phase+" low="+low+" value="+points[phase]+" high="+high);
			if(phase<2){
				assertThat(rows.get(rows.size()-1).end()).isNull();
				assertThat(intervals(true,id)).anyMatch(e->e.end()==null);
				tx(s->{Row current=s.find(type,id);assertThat(current.payload).isEqualTo(points[1]==null?"v1":"v2");});
			}
		}
		assertHistory(id,points[0],points[1],points[2]);tx(s->assertThat(s.find(type,id)).isNull());return points;
	}
	@Override public void close() throws Exception {
		try{writer.close();monitor.close();reader.close();}finally{scope.close();}
	}
	static final class Fixed implements ChangesetIdentifierSupplier<Instant> {
		Instant value=SEASONAL[0];int calls;
		@Override public Instant generateIdentifier(SharedSessionContract session){calls++;return value;}
	}
	@MappedSuperclass static abstract class Row {
		@Id int id;String payload="v1";
		@ElementCollection @CollectionTable(name="pp_tags",joinColumns=@JoinColumn(name="owner_id")) @Column(name="tag_value")
		@OrderColumn(name="item_order") @Temporal(rowStart="valid_from",rowEnd="valid_to",secondPrecision=6)
		List<String> tags=new ArrayList<>();
	}
	@Entity(name="ProviderPeriod") @Table(name="pp_period") @Temporal(rowStart="valid_from",rowEnd="valid_to",secondPrecision=6)
	static class Period extends Row {}
	@Entity(name="ProviderPeriodMillis") @Table(name="pp_period_millis") @Temporal(rowStart="valid_from",rowEnd="valid_to",secondPrecision=3)
	static class Millis extends Row {}
	@Entity(name="ProviderPeriodDefault") @Table(name="pp_period_default") @Temporal(rowStart="valid_from",rowEnd="valid_to")
	static class DefaultPrecision extends Row {}
}
