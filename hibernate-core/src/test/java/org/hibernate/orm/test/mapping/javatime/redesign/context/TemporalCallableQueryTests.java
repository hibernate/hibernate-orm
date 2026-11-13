package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.type.BasicType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Live callable boundaries distinguish ORM names from actual JDBC names.
///
/// @author Steve Ebersole
public class TemporalCallableQueryTests {
	static final Map<String,Object> OBSERVE=Map.of("hibernate.test.temporal.observe_callable",true,"hibernate.test.temporal.reuse_connection",true);

	@ParameterizedTest @ValueSource(booleans={true,false})
	void compositeShapesFailBeforeJdbc(boolean metadata) {
		try(var scope=ContextScope.open(metadata,OBSERVE,TemporalColumnQueryTests.Event.class)) {
			var entity=scope.factory.getJpaMetamodel().entity(TemporalColumnQueryTests.Event.class);
			for(String field:new String[]{"offsetStamp","zonedStamp","offsetClock"}) {
				var type=((SingularAttribute<?,?>)entity.getAttribute(field)).getType();
				scope.tx(s->{
					try(var call=s.createStoredProcedureCall("unsupported_column_call")) {
						assertThatThrownBy(()->call.registerParameter(1,type,ParameterMode.INOUT))
							.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("parameter 1")
							.hasMessageContaining("Multi-column").hasMessageContaining("single-column");
						assertThatThrownBy(()->call.registerParameter("p_value",type,ParameterMode.IN))
							.hasMessageContaining("parameter p_value").hasMessageContaining("single-column");
						assertThatThrownBy(()->call.markAsFunctionCall(type))
							.hasMessageContaining("function return").hasMessageContaining("single-column");
					}
				});
			}
			assertThat(scope.provider.callableMethods).isEmpty();
		}
	}

	@ParameterizedTest @ValueSource(booleans={true,false})
	void insufficientFunctionPairFailsBeforeJdbc(boolean metadata) {
		try(var scope=ContextScope.boot(metadata,true,OBSERVE,TemporalColumnQueryTests.Event.class)) {
			scope.provider.blocked=false;
			var type=new org.hibernate.type.internal.BasicTypeImpl<>(
					org.hibernate.type.descriptor.java.OffsetDateTimeJavaType.INSTANCE,
					org.hibernate.type.descriptor.jdbc.TimestampJdbcType.INSTANCE);
			try(var session=scope.factory.openSession();var call=session.createStoredProcedureCall("unsupported_return")) {
				assertThatThrownBy(()->call.markAsFunctionCall(type)).hasMessageContaining("function return")
					.hasMessageContaining("OffsetDateTime").hasMessageContaining("preservation");
			}
			assertThat(scope.provider.callableMethods).isEmpty();
		}
	}

	@ParameterizedTest @ValueSource(booleans={true,false})
	@SuppressWarnings("unchecked")
	void oracleModesAndObservedNames(boolean metadata) {
		assumeTrue(org.hibernate.cfg.Environment.getProperties().getProperty("hibernate.dialect","").contains("Oracle"));
		for(boolean passNames:new boolean[]{false,true}) {
			try(var scope=ContextScope.open(metadata,Map.of("hibernate.test.temporal.observe_callable",true,"hibernate.test.temporal.reuse_connection",true,"hibernate.query.pass_procedure_parameter_names",passNames),Event.class)) {
				var type=(BasicType<OffsetTime>)scope.factory.getMappingMetamodel().getEntityDescriptor(Event.class).getPropertyType("clock");
				scope.tx(s->s.doWork(c->{try(var st=c.createStatement()){st.execute("create or replace procedure temporal_qcc_probe(p_input in timestamp with time zone, p_output out timestamp with time zone, p_both in out timestamp with time zone) as begin p_output := p_input; end;");}}));
				try {
					for(boolean named:new boolean[]{false,true}) {
						for(OffsetTime value:new OffsetTime[]{OffsetTime.parse("01:00:00.123456+05:30:45"),null}) {
							scope.provider.callableMethods.clear();
							scope.tx(s->{try(var call=s.createStoredProcedureCall("temporal_qcc_probe")) {
								var in=named?call.registerParameter("p_input",type,ParameterMode.IN):call.registerParameter(1,type,ParameterMode.IN);
								var out=named?call.registerParameter("p_output",type,ParameterMode.OUT):call.registerParameter(2,type,ParameterMode.OUT);
								var both=named?call.registerParameter("p_both",type,ParameterMode.INOUT):call.registerParameter(3,type,ParameterMode.INOUT);
								call.setParameter(in,value);call.setParameter(both,value);
								Object expected=value==null?null:OffsetTime.parse("13:29:15.123456+18:00");
								assertThat(call.getOutputs().getOutputParameterValue(out)).isEqualTo(expected);
								assertThat(call.getOutputs().getOutputParameterValue(both)).isEqualTo(expected);
							}});
							// ORM names currently adapt to positions for binding, registration and extraction.
							boolean jdbcNamed=false;
							assertThat(scope.provider.callableMethods).anyMatch(m->m.equals("execute"));
							assertThat(scope.provider.callableMethods).anyMatch(m->m.startsWith("set")&&m.endsWith(jdbcNamed?":p_input":":1"));
							assertThat(scope.provider.callableMethods).anyMatch(m->m.startsWith("get")&&m.endsWith(jdbcNamed?":p_output":":2"));
							System.out.println("QCC_CALL metadata="+metadata+" passNames="+passNames+" named="+named+" methods="+scope.provider.callableMethods);
						}
					}
				}
				finally {scope.tx(s->s.doWork(c->{try(var st=c.createStatement()){st.execute("drop procedure temporal_qcc_probe");}}));}
			}
		}
	}

	@ParameterizedTest @ValueSource(booleans={true,false})
	@SuppressWarnings("unchecked")
	void mappedFunctionReturn(boolean metadata) {
		try(var scope=ContextScope.open(metadata,OBSERVE,Event.class)) {
			var type=(BasicType<OffsetDateTime>)scope.factory.getMappingMetamodel().getEntityDescriptor(Event.class).getPropertyType("stamp");
			var dialect=scope.factory.getJdbcServices().getDialect();
			// The explicit mapping uses the plain TIMESTAMP UTC carrier contributed by ContextScope.
			// Match that SQL representation; a TIMESTAMP WITH TIME ZONE signature would add a server cast.
			String create=dialect instanceof org.hibernate.dialect.OracleDialect
					?"create or replace function temporal_qcc_echo(p_value in timestamp) return timestamp as begin return p_value; end;"
					:dialect instanceof org.hibernate.dialect.PostgreSQLDialect
					?"create function temporal_qcc_echo(p_value timestamp) returns timestamp language sql as 'select p_value'"
					:"create alias temporal_qcc_echo for '"+TemporalCallableQueryTests.class.getName()+".echo'";
			String drop=dialect instanceof org.hibernate.dialect.H2Dialect?"drop alias temporal_qcc_echo":
					"drop function temporal_qcc_echo"+(dialect instanceof org.hibernate.dialect.PostgreSQLDialect?"(timestamp)":"");
			scope.tx(s->s.doWork(c->{try(var st=c.createStatement()){st.execute(create);}}));
			try {
				for(OffsetDateTime value:new OffsetDateTime[]{OffsetDateTime.parse("2026-07-01T12:00:00.123456+02:00"),null}) {
					scope.tx(s->{try(var call=s.createStoredProcedureCall("temporal_qcc_echo")) {
						call.markAsFunctionCall(type);
						var in=call.registerParameter(1,type,ParameterMode.IN);call.setParameter(in,value);
						assertThat(call.getSingleResult()).isEqualTo(value==null?null:OffsetDateTime.parse("2026-07-01T15:30:00.123456+05:30"));
					}});
				}
			}
			finally {scope.tx(s->s.doWork(c->{try(var st=c.createStatement()){st.execute(drop);}}));}
		}
	}
	@ParameterizedTest @ValueSource(booleans={true,false})
	@SuppressWarnings("unchecked")
	void postgresInputAndOutputBoundary(boolean metadata) {
		assumeTrue(org.hibernate.cfg.Environment.getProperties().getProperty("hibernate.dialect","").contains("PostgreSQL"));
		try(var scope=ContextScope.open(metadata,OBSERVE,PgEvent.class)) {
			var type=(BasicType<OffsetTime>)scope.factory.getMappingMetamodel().getEntityDescriptor(PgEvent.class).getPropertyType("clock");
			scope.tx(s->s.doWork(c->{try(var st=c.createStatement()){st.execute("create procedure temporal_qcc_pg(p_value timetz) language sql as 'insert into PgCallableEvent(id,clock) values(1,p_value)'");}}));
			try {
				for(boolean named:new boolean[]{false,true}) {
					for(OffsetTime value:new OffsetTime[]{OffsetTime.parse("01:00:00.123456+05:30:45"),null}) {
						scope.provider.callableMethods.clear();
						scope.tx(s->{try(var call=s.createStoredProcedureCall("temporal_qcc_pg")) {
							var p=named?call.registerParameter("p_value",type,ParameterMode.IN):call.registerParameter(1,type,ParameterMode.IN);
							call.setParameter(p,value);call.execute();
						}});
						assertThat(scope.provider.callableMethods).anyMatch(m->m.startsWith("set")&&m.endsWith(":1"));
						scope.tx(s->{assertThat(s.find(PgEvent.class,1).clock).isEqualTo(value);s.createMutationQuery("delete PgCallableEvent").executeUpdate();});
					}
					for(ParameterMode mode:new ParameterMode[]{ParameterMode.OUT,ParameterMode.INOUT}) {
						scope.provider.callableMethods.clear();
						scope.tx(s->{try(var call=s.createStoredProcedureCall("temporal_qcc_pg")) {
							var p=named?call.registerParameter("p_value",type,mode):call.registerParameter(1,type,mode);
							if(mode==ParameterMode.INOUT)call.setParameter(p,OffsetTime.parse("01:00+02:00"));
							assertThatThrownBy(call::getOutputs).hasMessageContaining("OUT/INOUT").hasMessageContaining("supported result-set mapping");
						}});
						assertThat(scope.provider.callableMethods).noneMatch(m->m.startsWith("set")||m.startsWith("execute"));
					}
				}
				scope.provider.callableMethods.clear();
				scope.tx(s->{try(var call=s.createStoredProcedureCall("temporal_qcc_pg")) {
					var p=call.registerParameter(1,type,ParameterMode.IN);call.setParameter(p,OffsetTime.parse("01:00+16:00"));
					assertThatThrownBy(call::getOutputs).hasMessageContaining("-15:59:59");
				}});
				assertThat(scope.provider.callableMethods).noneMatch(m->m.startsWith("set")||m.startsWith("execute"));
			}
			finally {scope.tx(s->s.doWork(c->{try(var st=c.createStatement()){st.execute("drop procedure temporal_qcc_pg(timetz)");}}));}
		}
	}
	@Entity(name="PgCallableEvent") static class PgEvent {
		@Id int id;
		@jakarta.persistence.Column(secondPrecision=6) OffsetTime clock;
	}

	public static java.time.LocalDateTime echo(java.time.LocalDateTime value) {return value;}
	@Entity(name="CallablePreservationEvent") static class Event {
		@Id int id;
		@TimeZoneAdjustment("+18:00") OffsetTime clock;
		@TimeZoneAdjustment("+05:30") OffsetDateTime stamp;
	}
}
