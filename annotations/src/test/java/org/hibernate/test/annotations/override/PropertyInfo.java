package org.hibernate.test.annotations.override;

import java.math.BigDecimal;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class PropertyInfo {
	public Integer parcelNumber;
	public Integer size;
	public BigDecimal tax;
}
