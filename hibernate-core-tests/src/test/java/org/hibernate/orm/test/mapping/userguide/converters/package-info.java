/// External converter registrations used by the User Guide examples.
///
/// @author Steve Ebersole
//tag::guide-converter-registrations[]
@ConverterRegistrations({
		@ConverterRegistration(converter = YesNoConverter.class),
		@ConverterRegistration(converter = PeriodStringConverter.class, autoApply = false)
})
package org.hibernate.orm.test.mapping.userguide.converters;

import org.hibernate.annotations.ConverterRegistration;
import org.hibernate.annotations.ConverterRegistrations;
import org.hibernate.type.YesNoConverter;
import org.hibernate.orm.test.mapping.converter.PeriodStringConverter;
//end::guide-converter-registrations[]
