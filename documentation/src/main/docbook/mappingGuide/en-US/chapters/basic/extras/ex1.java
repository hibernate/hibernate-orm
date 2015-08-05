@Entity
public class Product {
    @Id
	@Basic
	private Integer id;
	@Basic
	private String sku;
	@Basic
	private String name;
	@Basic
	private String description;
}