package org.hibernate.orm.test.mapping.javatime.redesign.context;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

import java.io.Serializable;
import java.time.ZonedDateTime;
import jakarta.persistence.*;
import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.CompositeTypeRegistration;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.mapping.Component;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

/// Ordinary registered and explicitly selected user composites retain their own
/// construction/copy/cache behavior beside contextual temporal implementations.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
class TemporalCustomCompositeContextTests {
	@ParameterizedTest @ValueSource(booleans={true,false})
	void customRegistrationAndTemporalSubclassAreNotContextualized(boolean metadata) {
		try(var scope=ContextScope.open(metadata,Control.class,TemporalContextLifecycleTests.Event.class)) {
			final var binding=scope.metadata.getEntityBinding(Control.class.getName());
			for(String name:new String[]{"label","value"}) {
				final var component=(Component)binding.getProperty(name).getValue();
				assertThat(component.getContextualCompositeUserType()).isNull();
				assertThat(component.copy().getType()).hasSameClassAs(component.getType());
			}
			assertThat(scope.factory.getMappingMetamodel().getEntityDescriptor(Control.class).canUseReferenceCacheEntries()).isTrue();
			final var row=new Control();row.id=1;row.label=new Label("control");row.value=TemporalContextLifecycleTests.INPUT;
			scope.tx(session->session.persist(row));
			scope.factory.getCache().evictAllRegions();
			scope.tx(session->{final var actual=session.find(Control.class,1);assertThat(actual.label).isEqualTo(row.label);assertThat(actual.value.toInstant()).isEqualTo(row.value.toInstant());});
			scope.measure();scope.tx(session->assertThat(session.find(Control.class,1).label).isEqualTo(row.label));scope.noSelect();
		}
	}
	public record Label(String text) {}
	public static class LabelMapping { String text; }
	public static class LabelType implements CompositeUserType<Label> {
		public Object getPropertyValue(Label value,int property){return value.text();}
		public Label instantiate(ValueAccess values){final var text=values.getValue(0,String.class);return text==null?null:new Label(text);}
		public Class<?> embeddable(){return LabelMapping.class;}
		public Class<Label> returnedClass(){return Label.class;}
		public boolean equals(Label first,Label second){return java.util.Objects.equals(first,second);}
		public int hashCode(Label value){return value.hashCode();}
		public Label deepCopy(Label value){return value;}
		public boolean isMutable(){return false;}
		public Serializable disassemble(Label value){return value==null?null:value.text();}
		public Label assemble(Serializable cached,Object owner){return cached==null?null:new Label((String)cached);}
		public Label replace(Label detached,Label managed,Object owner){return detached;}
	}
	public static class CustomZoned extends org.hibernate.usertype.internal.ZonedDateTimeCompositeUserType {}
	@Entity(name="ContextCustomComposite") @Immutable @Cache(usage=CacheConcurrencyStrategy.READ_ONLY)
	@CompositeTypeRegistration(embeddableClass=Label.class,userType=LabelType.class)
	static class Control {
		@Id int id;
		Label label;
		@CompositeType(CustomZoned.class) ZonedDateTime value;
	}
}
