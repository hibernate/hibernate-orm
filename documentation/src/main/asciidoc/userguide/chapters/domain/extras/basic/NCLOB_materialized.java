@Entity
public class Product {
	...
	@Lob
	@Basic
	@Nationalized
	public String description;
}