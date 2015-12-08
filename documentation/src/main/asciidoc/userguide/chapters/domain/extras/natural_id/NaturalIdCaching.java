@Entity
@NaturalIdCache
public class Company {
	@Id
	private Integer id;
	@NaturalId
	private String taxIdentifier;
	...
}