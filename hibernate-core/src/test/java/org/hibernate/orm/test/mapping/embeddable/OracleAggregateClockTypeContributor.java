package org.hibernate.orm.test.mapping.embeddable;

import java.time.LocalTime;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.dialect.type.internal.OracleLocalTimeJdbcType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.LocalTimeJavaType;
import org.hibernate.type.internal.BasicTypeImpl;

/// Gives the representation fixture's legacy query parameters the same explicit
/// clock handler as its attributes. Legacy default TIME binding loses fractions;
/// that unrelated parameter-resolution behavior is not the subject of this test.
///
/// @author Steve Ebersole
public class OracleAggregateClockTypeContributor implements TypeContributor {
	@Override
	public void contribute(TypeContributions contributions, ServiceRegistry registry) {
		contributions.getTypeConfiguration().getBasicTypeRegistry().register(
				new BasicTypeImpl<>( LocalTimeJavaType.INSTANCE, OracleLocalTimeJdbcType.INSTANCE ),
				StandardBasicTypes.LOCAL_TIME.getName(), LocalTime.class.getName() );
	}
}
