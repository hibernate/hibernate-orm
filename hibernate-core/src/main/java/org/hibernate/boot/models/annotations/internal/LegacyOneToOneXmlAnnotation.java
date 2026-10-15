package org.hibernate.boot.models.annotations.internal;

import jakarta.persistence.OneToOne;
import org.hibernate.models.spi.ModelsContext;

/// Retains pre-8.0 XML write semantics without adding an annotation attribute.
///
/// @author Steve Ebersole
public final class LegacyOneToOneXmlAnnotation extends OneToOneJpaAnnotation {
	public LegacyOneToOneXmlAnnotation(OneToOne source, ModelsContext modelsContext) {
		super( source, modelsContext );
	}
}
