package org.hibernate.orm.test.bootstrap.binding.annotations.override;

import java.math.BigDecimal;
import jakarta.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class PropertyInfo {
	public Integer parcelNumber;
	public Integer size;
	public BigDecimal tax;
}
