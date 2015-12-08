@Entity
public class Customer {
	@Id
	private Integer id;
	private String name;
	@Basic( fetch = FetchType.LAZY )
	private UUID accountsPayableXrefId;
	@Lob
	@Basic( fetch = FetchType.LAZY )
	@LazyGroup( "lobs" )
	private Blob image;

	...
}