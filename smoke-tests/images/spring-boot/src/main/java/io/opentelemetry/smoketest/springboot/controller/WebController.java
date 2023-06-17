/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.smoketest.springboot.controller;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
public class WebController {
  private static final Logger logger = LoggerFactory.getLogger(WebController.class);

  @RequestMapping("/greeting")
  public String greeting() {
    logger.info("HTTP request received");
    return withSpan();
  }

  @GetMapping("/snippetTest")
  public ModelAndView getHelloPage() {
    ModelAndView mav = new ModelAndView(); // the name of the HTML page
    mav.setViewName("test.html");
    return mav;
  }

  @WithSpan
  public String withSpan() {
    return "Hi!";
  }
}
