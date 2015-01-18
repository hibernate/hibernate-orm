package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@EnableAutoConfiguration
public class SampleController {

	@RequestMapping( "/" )
	@ResponseBody
	String home() {
		return "Hello World!";
	}

	public static void main( String[] args ) throws Exception {
		SpringApplication.run( SampleController.class, args );
	}
}
