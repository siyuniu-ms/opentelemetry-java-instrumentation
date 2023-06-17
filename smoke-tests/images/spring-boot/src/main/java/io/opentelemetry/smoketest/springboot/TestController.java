package io.opentelemetry.smoketest.springboot;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
public class TestController {

  @GetMapping("/test")
  public ModelAndView getHelloPage() {
    ModelAndView mav = new ModelAndView(); // the name of the HTML page
    mav.setViewName("test.html");
    return mav;
  }
}
