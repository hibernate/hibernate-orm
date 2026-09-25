package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.source.spi.AttributePath;

/**
 * @author Steve Ebersole
 * @author Dmytro Bondar
 */
public interface ImplicitIndexColumnNameSource extends ImplicitNameSource {

	AttributePath getPluralAttributePath();

}
