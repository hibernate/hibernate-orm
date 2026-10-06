package org.hibernate.orm.post;

/// Renders a deterministic, review-oriented API and SPI migration report.
///
/// @author Steve Ebersole
public final class ClassificationMigrationReportRenderer {
	public String render(ClassificationMigrationValidator.Result result) {
		final StringBuilder report = new StringBuilder();
		report.append( "Hibernate ORM migration compatibility: " )
				.append( result.hasFailures() ? "FAILED" : result.hasWarnings() ? "PASSED_WITH_WARNINGS" : "PASSED" )
				.append( "\n\n" )
				.append( "Baseline compatibility family: " ).append( result.getBaseline().getHibernateVersion() ).append( '\n' )
				.append( "Baseline source version: " ).append( result.getBaseline().getSourceVersion() ).append( '\n' )
				.append( "Baseline policy: " ).append( result.getBaselinePolicy() ).append( '\n' )
				.append( "Current compatibility family: " ).append( result.getCurrent().getHibernateVersion() ).append( '\n' )
				.append( "Current source version: " ).append( result.getCurrent().getSourceVersion() ).append( '\n' )
				.append( "Classification schema: " ).append( ClassificationMetadata.SCHEMA )
				.append( " v" ).append( ClassificationMetadata.SCHEMA_VERSION ).append( '\n' )
				.append( "API major-family compatibility: " ).append( result.isApiEnforced() ? "ENFORCED" : "NOT_APPLICABLE" ).append( '\n' )
				.append( "SPI X.Y-family compatibility: " ).append( result.isSpiEnforced() ? "ENFORCED" : "NOT_APPLICABLE" ).append( "\n\n" );

		if ( result.getBaselinePolicy() == ClassificationMigrationValidator.BaselinePolicy.ADVISORY_PRERELEASE ) {
			report.append( "Compatibility findings are nonblocking because the baseline is an Alpha/Beta/CR release.\n\n" );
		}
		report.append( "Diagnostics: " ).append( result.getDiagnostics().size() )
				.append( "; ERROR=" ).append( result.getDiagnosticCount( ClassificationMigrationValidator.Severity.ERROR ) )
				.append( "; REVIEW=" ).append( result.getDiagnosticCount( ClassificationMigrationValidator.Severity.REVIEW ) )
				.append( "\n\n" );

		if ( result.getDiagnostics().isEmpty() ) {
			return report.append( "No migration compatibility diagnostics.\n" ).toString();
		}
		for ( ClassificationMigrationValidator.Diagnostic diagnostic : result.getDiagnostics() ) {
			report.append( '[' ).append( diagnostic.getSeverity() ).append( "] " )
					.append( diagnostic.getSurface() == ClassificationMigrationValidator.Surface.API
							? "API_COMPATIBILITY_REGRESSION"
							: "SPI_COMPATIBILITY_REGRESSION" )
					.append( '\n' )
					.append( "  Element: " ).append( diagnostic.getElementId() ).append( '\n' )
					.append( "  Cause: " ).append( diagnostic.getJavaCause() == null
							? diagnostic.getFindingCause()
							: diagnostic.getJavaCause() ).append( '\n' )
					.append( "  Classification: " ).append( category( diagnostic.getBaselineCategory() ) )
					.append( " -> " ).append( category( diagnostic.getCurrentCategory() ) ).append( '\n' )
					.append( "  Roles: " ).append( diagnostic.getRoles().isEmpty() ? "none" : diagnostic.getRoles() ).append( '\n' )
					.append( "  Impacts: " ).append( diagnostic.getImpacts() ).append( '\n' )
					.append( "  Message: " ).append( diagnostic.getMessage() ).append( "\n\n" );
		}
		return report.toString();
	}

	private static String category(ClassificationModel.Category category) {
		return category == null ? "ABSENT_OR_UNSUPPORTED" : category.name();
	}
}
